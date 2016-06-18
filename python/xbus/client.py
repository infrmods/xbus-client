import json
import requests
from .error import XBusError, DeadlineExceededError
from .ldict import LDict


class Config(object):
    def __init__(self, name, value, version):
        self.name = name
        self.value = value
        self.version = version

    @classmethod
    def from_dict(Config, d):
        return Config(d['name'], d['value'], d['version'])

    def __repr__(self):
        return '<Config: %s, version: %d>' % (self.name, self.version)


class ConfigMix(object):
    def __init__(self):
        self._config_revisions = LDict(True)
        super(ConfigMix, self).__init__()

    def get_config(self, name):
        result = self._request('GET', '/api/configs/%s' % name)
        self._config_revisions[name] = result['revision']
        return Config.from_dict(result['config'])

    def put_config(self, name, value, version=None):
        data = dict(value=value)
        if version:
            data['version'] = version
        result = self._request('PUT', '/api/configs/%s' % name, data=data)
        self._config_revisions[name] = result['revision']

    def watch_config(self, name, revision=None, timeout=None):
        params = {}
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
        return '%s:%s' % (self.name, self.version)

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
        return '<Service: %s>' % self.key


class ServiceMix(object):
    def __init__(self):
        self._service_revisions = LDict(True)
        self._lease_ids = LDict()
        self._addrs = LDict()

    def get_service(self, name, version):
        result = self._request('GET', '/api/services/%s/%s' % (name, version))
        self._service_revisions[name] = result['revision']
        return Service.from_dict(name, version, result['service'])

    def plug_service(self, service, ttl='60s', lease_id=None):
        if len(service.endpoints) != 1:
            raise ValueError('endpoints\'s size must be 1')
        data = dict(desc=json.dumps(service.desc()),
                    endpoint=json.dumps(service.endpoints[0].to_dict()))
        if ttl:
            data['ttl'] = ttl
        if lease_id:
            data['lease_id'] = lease_id
        result = self._request('POST', '/api/services/%s/%s' % (service.name, service.version),
                               data=data)
        self._lease_ids[service.key] = result['lease_id']
        self._addrs[service.key] = service.endpoints[0].address
        return lease_id

    def plug_services(self, services, endpoint, ttl=None, lease_id=None):
        data = dict(endpoint=endpoint, desces=[x.desc() for x in services])
        if ttl:
            data['ttl'] = ttl
        if lease_id:
            data['lease_id'] = lease_id
        result = self._request('POST', '/api/services', data=data)
        return result['lease_id']

    def unplug_service(self, name, version):
        key = '%s:%s' % (name, version)
        addr = self._addrs.get(key, None)
        if addr is None:
            raise Exception('not plugged: %s' % key)
        self._request('DELETE', '/api/services/%s/%s/%s' % (name, version, addr))
        del self._lease_ids['%s:%s' % (name, version)]

    def keepalive_service(self, name, version):
        key = '%s:%s' % (name, version)
        lease_id = self._lease_ids.get(key, None)
        if lease_id is None:
            raise Exception('%s is not pulgged' % key)
        self._request('POST', '/api/leases/%d' % lease_id)

    def update_service(self, service):
        if len(service.endpoints) != 1:
            raise ValueError('endpoints\'s size must be 1')
        addr = self._addrs.get(service.key, None)
        if addr is None:
            raise Exception('not plugged: %s' % service.key)
        data = dict(endpoint=json.dumps(service.endpoints[0].to_dict()))
        self._request('PUT',
                      '/api/services/%s/%s/%s' % (service.name, service.version, addr),
                      data=data)

    def watch_service(self, name, version, revision=None, timeout=None):
        params = {}
        if revision is None:
            revision = self._service_revisions.get('%s:%s' % (name, version), 0)
            if revision:
                revision += 1
        if revision:
            params['revision'] = revision
        while True:
            try:
                result = self._request('GET', '/api/services/%s/%s' % (name, version),
                                       params=params)
            except DeadlineExceededError:
                if timeout:
                    return
                continue
            self._service_revisions[name] = result['revision']
            return Service.from_dict(name, version, result['service'])

    def service_session(self, ttl=None):
        return ServiceSession(self=None)


class ServiceSession(object):
    def __init__(self, client, ttl=None):
        self.client = client
        self.ttl = ttl
        self.lease_id = None

    def _wrap_call(self, f, *argv, **kwargs):
        if self.lease_id is not None:
            kwargs['lease_id'] = self.lease_id
            if f(*argv, **kwargs) != self.lease_id:
                raise Exception('new lease generated')
        else:
            if self.ttl is not None:
                kwargs['ttl'] = self.ttl
            self.lease_id = f(*argv, **kwargs)

    def plug_service(self, service):
        self._wrap_call(self.client.plug_service, service)

    def plug_services(self, services):
        self._wrap_call(self.client.plug_services, services)

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
    def __init__(self, endpoint, cert='appcert.pem', key='appkey.pem', verify='cacert.pem'):
        self.endpoint = endpoint
        self.cert = cert
        self.key = key
        self.verify = verify
        super(XBusClient, self).__init__()

    def _request(self, method, path, params=None, data=None):
        rep = requests.request(method, self.endpoint + path, params=params, data=data,
                               cert=(self.cert, self.key), verify=self.verify)
        result = rep.json()
        if result['ok']:
            return result.get('result', None)
        raise XBusError.new_error(result['error']['code'], result['error'].get('message', None))

    def revoke_lease(self, lease_id):
        self._request('DELETE', '/api/services', params=dict(lease_id=lease_id))

    def keepalive_lease(self, lease_id):
        self._request('POST', '/api/leases/%d' % lease_id)
