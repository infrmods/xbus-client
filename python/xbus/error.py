
class ErrorMeta(type):
    def __new__(meta, name, bases, attrs):
        newcls = super(ErrorMeta, meta).__new__(meta, name, bases, attrs)
        if not hasattr(newcls, '__codes__'):
            newcls.__codes__ = {}
        if 'CODE' in attrs:
            newcls.__codes__[attrs['CODE']] = newcls
        return newcls


class XBusError(Exception, metaclass=ErrorMeta):
    def __init__(self, code, msg):
        self.code = code
        self.message = msg
        super(XBusError, self).__init__(self, msg or code)

    @classmethod
    def new_error(cls, code, msg):
        dest_cls = cls.__codes__.get(code, None)
        if dest_cls:
            return dest_cls(msg)
        return cls(code, msg)

    def __repr__(self):
        if not self.message:
            return 'XBusError<%s>' % self.code
        return 'XBusError<%s: %s>' % (self.code, self.message)


class SimpleError(XBusError):
    def __init__(self, msg):
        super(SimpleError, self).__init__(self.CODE, msg)


class SystemError(SimpleError):
    CODE = 'SYSTEM_ERROR'


class InvalidVersionError(SimpleError):
    CODE = 'INVALID_VERSION'


class NotFoundError(SimpleError):
    CODE = 'NOT_FOUND'


class DeadlineExceededError(SimpleError):
    CODE = 'DEADLINE_EXCEEDED'


class NotPermittedError(SimpleError):
    CODE = 'NOT_PERMITTED'


class InvalidNameError(SimpleError):
    CODE = 'INVALID_NAME'


class InvalidValueError(SimpleError):
    CODE = 'INVALID_VALUE'


class NameDuplicatedError(SimpleError):
    CODE = 'NAME_DUPLICATED'
