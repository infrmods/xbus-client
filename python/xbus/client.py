import json
import os

import requests

from .error import DeadlineExceededError, XBusError
from .ldict import LDict


class Config(object):
    def __init__(self, name, value, version, tag=None):
        self.name = name
        self.value = value
        self.version = version
        self.tag = tag

    @classmethod
    def from_dict(Config, d):
        return Config(d['name'], d['value'], d['version'], d.get('tag', None))

    def __repr__(self):
        return '<Config: %s, version: %d>' % (self.name, self.version)

    def dump(self):
        return dict(name=self.name,
                    value=self.value,
                    version=self.version,
                    tag=self.tag)


class Configs(object):
    def __init__(self, total, configs, skip, limit):
        self.total = total
        self.configs = configs
        self.skip = skip
        self.limit = limit

    def __len__(self):
        return len(self.configs)

    def __iter__(self):
        for config in self.configs:
            yield config


class ConfigMix(object):
    def __init__(self):
        self._config_revisions = LDict(True)
        super(ConfigMix, self).__init__()

    def list_config(self, tag='', prefix='', skip=None, limit=None):
        url = '/api/configs?tag=%s&prefix=%s' % (tag, prefix)
        if skip is not None:
            url += '&skip=%d' % skip
        if limit is not None:
            url += '&limit=%s' % limit
        result = self._request('GET', url)
        return Configs(result['total'], result['configs'], result['skip'],
                       result['limit'])

    def get_configs(self, *keys):
        url = '/api/configs?keys=%s' % json.dumps(keys)
        result = self._request('GET', url)
        return {
            item['name']: Config.from_dict(item)
            for item in result['configs']
        }

    def get_config(self, name):
        result = self._request('GET', '/api/configs/%s' % name)
        self._config_revisions[name] = result['revision']
        return Config.from_dict(result['config'])

    def put_config(self, name, value, version=None, tag=None, remark=None):
        data = dict(value=value)
        if version:
            data['version'] = version
        if tag:
            data['tag'] = tag
        if remark:
            data['remark'] = remark
        result = self._request('PUT', '/api/configs/%s' % name, data=data)
        self._config_revisions[name] = result['revision']

    def del_config(self, name):
        self._request('DELETE', '/api/configs/%s' % name)

    def watch_config(self, name, revision=None, timeout=None):
        params = dict(watch='true')
        if revision is None:
            revision = self._cofig_revisions.get(name, 0)
            if revision:
                revision += 1
        if revision:
            params['revision'] = revision
        if timeout:
            params['timeout'] = timeout

        while True:
            try:
                result = self._request('GET',
                                       '/api/configs/%s' % name,
                                       params=params)
            except DeadlineExceededError:
                if timeout:
                    return
                continue
            self._config_revisions[name] = result['revision']
            return Config.from_dict(result['config'])


class ServiceEndpoint(object):
    def __init__(self, address, config=None):
        self.address = address
        self.config = config

    def dump(self):
        d = dict(address=self.address)
        if self.config:
            d['config'] = self.config
        return d

    def __repr__(self):
        return '<ServiceEndpoint: %s>' % self.address


class ZoneService(object):
    def __init__(self,
                 service,
                 type,
                 zone='default',
                 proto=None,
                 description=None,
                 endpoints=None):
        self.service = service
        self.type = type
        self.zone = zone
        self.proto = proto
        self.description = description
        self.endpoints = []
        if endpoints:
            for endpoint in endpoints:
                if isinstance(endpoint, ServiceEndpoint):
                    self.endpoints.append(endpoint)
                else:
                    self.endpoints.append(ServiceEndpoint(**endpoint))

    def dump(self):
        return dict(service=self.service,
                    type=self.type,
                    proto=self.proto,
                    description=self.description,
                    endpoints=[e.dump() for e in self.endpoints])


class Service(object):
    def __init__(self, service, zones=None):
        self.service = service
        self.zones = {}
        if zones:
            for zone, service in zones.items():
                if isinstance(service, ZoneService):
                    self.zones[zone] = service
                else:
                    self.zones[zone] = ZoneService(**service)

    def dump(self):
        return {'service': self.service, 'zones': {k: v.dump() for k, v in self.zones.items()}}


class ServiceMix(object):
    def __init__(self):
        self._service_revisions = LDict(True, default=0)
        self._lease_ids = LDict(default=None)

    def get_service(self, service):
        result = self._request('GET', '/api/v1/services/%s' % service)
        self._service_revisions[service] = result['revision']
        return Service(**result['service'])

    def search_service(self, name, skip=0, limit=20):
        result = self._request(
            'GET',
            '/api/v1/services?q=%s&skip=%d&limit=%d' % (name, skip, limit))
        return result

    def plug_service(self, service, endpoint, ttl=None, lease_id=None):
        assert isinstance(service, ZoneService)
        data = dict(desc=json.dumps(service.dump()),
                    endpoint=json.dumps(endpoint.dump()))
        if ttl:
            data['ttl'] = ttl
        if lease_id:
            data['lease_id'] = lease_id
        result = self._request('POST',
                               '/api/v1/services/%s' % service.service,
                               data=data)
        self._lease_ids[service.service] = lease_id = result['lease_id']
        return result

    def plug_services(self, services, endpoint, ttl=None, lease_id=None):
        for service in services:
            assert isinstance(service, ZoneService)
        data = dict(endpoint=endpoint, desces=[x.dump() for x in services])
        if ttl:
            data['ttl'] = ttl
        if lease_id:
            data['lease_id'] = lease_id
        result = self._request('POST', '/api/v1/services', data=data)
        lease_id = result['lease_id']
        for service in services:
            self._lease_ids[service.service] = lease_id
        return result

    def keepalive_service(self, service):
        lease_id = self._lease_ids[service]
        if lease_id is None:
            raise Exception('%s is not pulgged' % service)
        self._request('POST', '/api/leases/%d' % lease_id)

    def watch_service(self, service, revision=None, timeout=None):
        params = dict(watch='true')
        if revision is None:
            revision = self._service_revisions[service]
            if revision:
                revision += 1
        if revision:
            params['revision'] = revision
        if timeout:
            params['timeout'] = timeout
        while True:
            try:
                result = self._request('GET',
                                       '/api/v1/services/%s' % service,
                                       params=params)
            except DeadlineExceededError:
                if timeout:
                    return
                continue
            self._service_revisions[service] = result['revision']
            return Service.from_dict(service, result['service'])

    def service_session(self, ttl=None):
        return ServiceSession(self, ttl)


class ServiceSession(object):
    def __init__(self, client, ttl=None):
        self.client = client
        self.ttl = ttl
        self.lease_id = None

    def _wrap_call(self, f, *argv, **kwargs):
        if self.lease_id is not None:
            kwargs['lease_id'] = self.lease_id
            result = f(*argv, **kwargs)
            if result['lease_id'] != self.lease_id:
                raise Exception('new lease generated')
        else:
            if self.ttl is not None:
                kwargs['ttl'] = self.ttl
            result = f(*argv, **kwargs)
            self.lease_id = result['lease_id']
            self.ttl = result['ttl']

    def plug_service(self, service, endpoint, **kwargs):
        self._wrap_call(self.client.plug_service, service, endpoint, **kwargs)

    def plug_services(self, services, endpoint, **kwargs):
        self._wrap_call(self.client.plug_services, services, endpoint,
                        **kwargs)

    def keepalive(self):
        if self.lease_id is not None:
            self.client.keepalive_lease(self.lease_id)

    def close(self):
        if self.lease_id is not None:
            self.client.revoke_lease(self.lease_id)
            self.lease_id = None


class AppMix(object):
    def list_app(self, skip=None, limit=20):
        params = {'limit': limit}
        if skip is not None:
            params['skip'] = skip
        result = self._request('GET', '/api/apps', params=params)
        return result

    def add_app(self, name, description, key_bits=2048, days=3650):
        data = dict(name=name,
                    description=description,
                    key_bits=key_bits,
                    days=days)
        result = self._request('PUT', '/api/apps', data=data)
        return result


class XBusClient(ConfigMix, ServiceMix, AppMix):
    def __init__(self,
                 endpoint,
                 cert=None,
                 key=None,
                 dev_app=None,
                 verify=None):
        if not dev_app:
            if key is None and cert is None:
                app_name = os.environ.get('APP_NAME', None)
                if app_name:
                    dev_app = app_name
        if verify is None and os.path.exists('cacert.pem'):
            verify = 'cacert.pem'
        self.endpoint = endpoint
        self.cert = cert
        self.key = key
        self.verify = verify
        self.dev_app = dev_app
        super(XBusClient, self).__init__()

    def _request(self, method, path, params=None, data=None):
        headers = {}
        if self.dev_app:
            headers['Dev-App'] = self.dev_app
        rep = requests.request(method,
                               self.endpoint + path,
                               params=params,
                               data=data,
                               cert=(self.cert, self.key),
                               verify=self.verify,
                               headers=headers)
        result = rep.json()
        if result['ok']:
            return result.get('result', None)
        raise XBusError.new_error(result['error']['code'],
                                  result['error'].get('message', None))

    def revoke_lease(self, lease_id):
        self._request('DELETE', '/api/leases/%d' % lease_id)

    def keepalive_lease(self, lease_id):
        self._request('POST', '/api/leases/%d' % lease_id)
