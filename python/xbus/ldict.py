from threading import Lock


_None = type('_None', (object,), {})()


class LDict(object):
    def __init__(self, compare=False, default=_None, key_func=None):
        self._lk = Lock()
        self.compare = compare
        self.values = {}
        self.default = default
        self.key_func = key_func

    def __getitem__(self, key):
        if self.key_func is not None:
            key = self.key_func(key)
        with self._lk:
            if self.default is not _None:
                return self.values.get(key, self.default)
            return self.values[key]

    def get(self, key, d):
        if self.key_func is not None:
            key = self.key_func(key)
        with self._lk:
            return self.values.get(key, d)

    def __setitem__(self, key, value):
        if self.key_func is not None:
            key = self.key_func(key)
        with self._lk:
            if self.compare:
                old = self.values.get(key, 0)
                if old >= value:
                    return
            self.values[key] = value

    def __delitem__(self, key):
        if self.key_func is not None:
            key = self.key_func(key)
        with self._lk:
            del self.values[key]

    def __contains__(self, key):
        if self.key_func is not None:
            key = self.key_func(key)
        return key in self.values
