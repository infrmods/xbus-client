from threading import Lock


class LDict(object):
    def __init__(self, compare=False):
        self._lk = Lock()
        self.compare = compare
        self.values = {}

    def __getitem__(self, key):
        with self._lk:
            return self.values[key]

    def get(self, key, d):
        with self._lk:
            return self.values.get(key, d)

    def __setitem__(self, key, value):
        with self._lk:
            if self.compare:
                old = self.values.get(key, 0)
                if old >= value:
                    return
            self.values[key] = value

    def __delitem__(self, key):
        with self._lk:
            self.values.pop(key, None)
