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
        endpoints = [ServiceEndpoint(x['address'], x.get('config', None)) for x in d['endpoints']]
        return Service(name, version, d['type'],
                       d.get('proto', None), d.get('description', None),
                       endpoints)

    def desc(self):
        d = dict(type=self.type)
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
        self._keep_ids = LDict()
        self._addrs = LDict()

    def get_service(self, name, version):
        result = self._request('GET', '/api/services/%s/%s' % (name, version))
        self._service_revisions[name] = result['revision']
        return Service.from_dict(name, version, result['service'])

    def plug_service(self, service, ttl=None):
        if len(service.endpoints) != 1:
            raise ValueError('endpoints\'s size must be 1')
        data = dict(desc=json.dumps(service.desc()),
                    endpoint=json.dumps(service.endpoints[0].to_dict()))
        if ttl:
            data['ttl'] = ttl
        result = self._request('POST', '/api/services/%s/%s' % (service.name, service.version),
                               data=data)
        self._keep_ids[service.key] = result['keep_id']
        self._addrs[service.key] = service.endpoints[0].address

    def unplug_service(self, name, version):
        key = '%s:%s' % (name, version)
        addr = self._addrs.get(key, None)
        if addr is None:
            raise Exception('not plugged: %s' % key)
        self._request('DELETE', '/api/services/%s/%s/%s' % (name, version, addr))
        del self._keep_ids['%s:%s' % (name, version)]

    def keepalive_service(self, name, version):
        key = '%s:%s' % (name, version)
        keep_id = self._keep_ids.get(key, None)
        if keep_id is None:
            raise Exception('%s is not pulgged' % key)
        addr = self._addrs.get(key, None)
        if addr is None:
            raise Exception('not plugged: %s' % key)
        data = dict(keep_id=keep_id)
        self._request('PUT',
                      '/api/services/%s/%s/%s' % (name, version, addr),
                      data=data)

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
