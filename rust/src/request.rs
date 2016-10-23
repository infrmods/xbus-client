use std::io::Read;
use std::collections::HashMap;
use hyper::client::Client as HttpClient;
use hyper::header::Headers;
use hyper::method::Method;
use hyper::status::StatusCode;
use hyper::Url;
use serde::Deserialize;
use serde_json::from_reader;
use error::Error;


pub struct RequestBuilder<'a> {
    client: &'a HttpClient,
    endpoint: &'a str,
    method: Method,
    path: &'a str,
    params: Option<HashMap<&'a str, &'a str>>,
}


impl<'a> RequestBuilder<'a> {
    pub fn new(client: &'a HttpClient,
               endpoint: &'a str,
               method: Method,
               path: &'a str)
               -> RequestBuilder<'a> {
        RequestBuilder {
            client: client,
            endpoint: endpoint,
            method: method,
            path: path,
            params: None,
        }
    }

    pub fn param(mut self, name: &'a str, value: &'a str) -> RequestBuilder<'a> {
        if let Some(ref mut params) = self.params {
            params.insert(name, value);
        } else {
            let mut params = HashMap::new();
            params.insert(name, value);
            self.params = Some(params);
        }
        self
    }

    pub fn send<T: Deserialize>(self) -> Result<T, Error> {
        let mut url_str = self.endpoint.to_owned();
        url_str.push_str(self.path);
        let mut url = try!(Url::parse(&url_str));
        if let Some(ref params) = self.params {
            let mut pairs = url.query_pairs_mut();
            for (k, v) in params {
                pairs.append_pair(k, v);
            }
        }
        let mut resp = try!(self.client.request(self.method, url).send());
        if resp.status != StatusCode::Ok {
            let mut msg = format!("[{}]", resp.status).to_owned();
            try!(resp.read_to_string(&mut msg));
            return Err(Error::from(msg));
        }
        let json_rep: Response<T> = try!(from_reader(resp));
        json_rep.get()
    }
}


#[derive(Deserialize, Debug)]
struct RespError {
    pub code: String,
    pub message: Option<String>,
}


#[derive(Deserialize, Debug)]
struct Response<T: Deserialize> {
    pub ok: bool,
    pub result: Option<T>,
    pub error: Option<RespError>,
}

impl<T: Deserialize> Response<T> {
    fn get(self) -> Result<T, Error> {
        if self.ok {
            match self.result {
                Some(t) => Ok(t),
                None => Err(Error::from("missing result")),
            }
        } else {
            match self.error {
                Some(err) => Err(Error::Request(err.code, err.message.unwrap_or("".to_owned()))),
                None => Err(Error::from("missing error")),
            }
        }
    }
}
