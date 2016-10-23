use std::error::Error as StdError;
use std::fmt::{Display, Formatter, Error as FmtError};
use std::io::Error as IOError;
use openssl::ssl::error::SslError;
use serde_json::error::Error as JsonError;
use url::ParseError;
use hyper::Error as HttpError;


#[derive(Debug)]
pub enum Error {
    Io(IOError),
    Http(String),
    Ssl(String),
    Json(String),
    Request(String, String),
    Other(String),
}

impl Display for Error {
    fn fmt(&self, f: &mut Formatter) -> Result<(), FmtError> {
        match *self {
            Error::Io(ref e) => e.fmt(f),
            Error::Http(ref e) => write!(f, "{}", e),
            Error::Ssl(ref e) => write!(f, "{}", e),
            Error::Json(ref e) => write!(f, "{}", e),
            Error::Request(ref code, ref message) => {
                write!(f, "request fail[{}]: {}", code, message)
            }
            Error::Other(ref e) => write!(f, "{}", e),
        }
    }
}

impl StdError for Error {
    fn description(&self) -> &str {
        match *self {
            Error::Io(ref e) => e.description(),
            Error::Http(ref e) => e,
            Error::Ssl(ref e) => e,
            Error::Json(ref e) => e,
            Error::Request(_, ref message) => message,
            Error::Other(ref e) => e,
        }
    }
}

impl From<IOError> for Error {
    fn from(err: IOError) -> Error {
        Error::Io(err)
    }
}

impl From<HttpError> for Error {
    fn from(err: HttpError) -> Error {
        match err {
            HttpError::Io(e) => Error::Io(e),
            HttpError::Ssl(e) => Error::Ssl(format!("{}", e)),
            _ => Error::Other(err.description().to_owned()),
        }
    }
}

impl From<SslError> for Error {
    fn from(err: SslError) -> Error {
        Error::Ssl(format!("{}", err))
    }
}

impl From<JsonError> for Error {
    fn from(err: JsonError) -> Error {
        match err {
            JsonError::Io(err) => Error::Io(err),
            _ => Error::Json(format!("{}", err)),
        }
    }
}

impl From<ParseError> for Error {
    fn from(err: ParseError) -> Error {
        Error::Other(format!("parse url fail: {}", err))
    }
}

impl From<String> for Error {
    fn from(msg: String) -> Error {
        Error::Other(msg)
    }
}

impl<'a> From<&'a str> for Error {
    fn from(msg: &'a str) -> Error {
        Error::Other(msg.to_owned())
    }
}
