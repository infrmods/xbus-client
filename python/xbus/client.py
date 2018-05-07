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
        return dict(name=self.name, value=self.value,
                    version=self.version, tag=self.tag)


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
        return Configs(result['total'], result['configs'],
                       result['skip'], result['limit'])

    def get_configs(self, *keys):
        url = '/api/configs?keys=%s' % json.dumps(keys)
        result = self._request('GET', url)
        return {item['name']: Config.from_dict(item) for item in result['configs']}

    def get_config(self, name):
        result = self._request('GET', '/api/configs/%s' % name)
        self._config_revisions[name] = result['revision']
        return Config.from_dict(result['config'])

    def put_config(self, name, value, version=None, tag=None):
        data = dict(value=value)
        if version:
            data['version'] = version
        if tag:
            data['tag'] = tag
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
                result = self._request('GET', '/api/configs/%s' % name, params=params)
            except DeadlineExceededError:
                if timeout:
                    return
                continue
            self._config_revisions[name] = result['revision']
            return Config.from_dict(result['config'])


class ServiceEndpoint(object):
    def __init__(self, addr, config):
        self.address = addr
        self.config = config

    def to_dict(self):
        d = dict(address=self.address)
        if self.config:
            d['config'] = self.config
        return d

    def __repr__(self):
        return '<ServiceEndpoint: %s>' % self.address


class Service(object):
    def __init__(self, name, version, typ, proto=None, description=None, endpoints=None):
        self.name = name
        self.version = version
        self.type = typ
        self.proto = proto
        self.description = description
        self.endpoints = endpoints or []

    @property
    def key(self):
        return self.name, self.version

    @classmethod
    def from_dict(Service, name, version, d):
        if name != d['name'] or version != d['version']:
            raise Exception('invalid service: %r' % d)
        endpoints = [ServiceEndpoint(x['address'], x.get('config', None)) for x in d['endpoints']]
        return Service(name, version, d['type'],
                       d.get('proto', None), d.get('description', None),
                       endpoints)

    def desc(self):
        d = dict(name=self.name, version=self.version, type=self.type)
        if self.proto:
            d['proto'] = self.proto
        if self.description:
            d['description'] = self.description
        return d

    def __repr__(self):
        return '<Service %s:%s>' % self.key


class ServiceMix(object):
    def __init__(self):
        self._service_revisions = LDict(True, default=0, key_func=lambda x: '%s:%s' % x)
        self._lease_ids = LDict(default=None, key_func=lambda x: '%s:%s' % x)
        self._addrs = LDict(default=None, key_func=lambda x: '%s:%s' % x)

    def get_versions(self, name):
        result = self._request('GET', '/api/services/%s' % name)
        return {k: Service.from_dict(name, k, v) for k, v in result['services'].items()}

    def get_service(self, name, version):
        result = self._request('GET', '/api/services/%s/%s' % (name, version))
        self._service_revisions[name, version] = result['revision']
        return Service.from_dict(name, version, result['service'])

    def search_service(self, name, skip=0, limit=20):
        result = self._request('GET', '/api/services?q=%s&skip=%d&limit=%d' % (name, skip, limit))
        return result

    def plug_service(self, service, endpoint, ttl=None, lease_id=None):
        data = dict(desc=json.dumps(service.desc()),
                    endpoint=json.dumps(endpoint.to_dict()))
        if ttl:
            data['ttl'] = ttl
        if lease_id:
            data['lease_id'] = lease_id
        result = self._request('POST', '/api/services/%s/%s' % (service.name, service.version),
                               data=data)
        self._lease_ids[service.key] = lease_id = result['lease_id']
        self._addrs[service.key] = endpoint
        return result

    def plug_services(self, services, endpoint, ttl=None, lease_id=None):
        data = dict(endpoint=endpoint, desces=[x.desc() for x in services])
        if ttl:
            data['ttl'] = ttl
        if lease_id:
            data['lease_id'] = lease_id
        result = self._request('POST', '/api/services', data=data)
        lease_id = result['lease_id']
        for service in services:
            self._lease_ids[service.key] = lease_id
            self._addrs[service.key] = endpoint.address
        return result

    def unplug_service(self, name, version):
        addr = self._addrs[name, version]
        if addr is None:
            raise Exception('not plugged: %s:%s' % (name, version))
        self._request('DELETE', '/api/services/%s/%s/%s' % (name, version, addr))
        del self._lease_ids[name, version]
        del self._addrs[name, version]

    def keepalive_service(self, name, version):
        lease_id = self._lease_ids[name, version]
        if lease_id is None:
            raise Exception('%s:%s is not pulgged' % (name, version))
        self._request('POST', '/api/leases/%d' % lease_id)

    def update_service(self, service):
        if len(service.endpoints) != 1:
            raise ValueError('endpoints\'s size must be 1')
        addr = self._addrs[service.key]
        if addr is None:
            raise Exception('not plugged: %R' % service)
        data = dict(endpoint=json.dumps(service.endpoints[0].to_dict()))
        self._request('PUT',
                      '/api/services/%s/%s/%s' % (service.name, service.version, addr),
                      data=data)

    def watch_service(self, name, version, revision=None, timeout=None):
        params = dict(watch='true')
        if revision is None:
            revision = self._service_revisions[name, version]
            if revision:
                revision += 1
        if revision:
            params['revision'] = revision
        if timeout:
            params['timeout'] = timeout
        while True:
            try:
                result = self._request('GET', '/api/services/%s/%s' % (name, version),
                                       params=params)
            except DeadlineExceededError:
                if timeout:
                    return
                continue
            self._service_revisions[name, version] = result['revision']
            return Service.from_dict(name, version, result['service'])

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
        self._wrap_call(self.client.plug_services, services, endpoint, **kwargs)

    def unplug_service(self, name, version):
        self.client.unplug_service(name, version)

    def keepalive(self):
        if self.lease_id is not None:
            self.client.keepalive_lease(self.lease_id)

    def close(self):
        if self.lease_id is not None:
            self.client.revoke_lease(self.lease_id)
            self.lease_id = None


class XBusClient(ConfigMix, ServiceMix):
    def __init__(self, endpoint, cert=None, key=None,
                 dev_app=None, verify=None):
        if not dev_app:
            cert = cert or 'appcert.pem'
            key = key or 'appkey.pem'
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
        rep = requests.request(method, self.endpoint + path, params=params, data=data,
                               cert=(self.cert, self.key), verify=self.verify,
                               headers=headers)
        result = rep.json()
        if result['ok']:
            return result.get('result', None)
        raise XBusError.new_error(result['error']['code'], result['error'].get('message', None))

    def revoke_lease(self, lease_id):
        self._request('DELETE', '/api/leases/%d' % lease_id)

    def keepalive_lease(self, lease_id):
        self._request('POST', '/api/leases/%d' % lease_id)
