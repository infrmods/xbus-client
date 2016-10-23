use hyper::client::Client as HttpClient;
use hyper::net::{OpensslClient, HttpsConnector};
use hyper::method::Method;
use openssl::ssl;
use openssl::ssl::error::SslError;
use openssl::x509::{X509FileType, X509StoreContext};
use error::Error;
use request::RequestBuilder;


#[derive(Deserialize, Serialize, Debug)]
pub struct Config {
    endpoint: String,
    ca_file: Option<String>,
    cert_key_file: Option<(String, String)>,
}

pub struct XBusClient {
    config: Config,
    client: HttpClient,
}

impl XBusClient {
    pub fn new(config: Config) -> Result<XBusClient, Error> {
        ssl::init();
        let mut ssl_ctx: ssl::SslContext = try!(ssl::SslContext::new(ssl::SslMethod::Tlsv1_2));
        if let Some(ref ca_file) = config.ca_file {
            try!(ssl_ctx.set_CA_file(ca_file.as_str()));
        }
        if let Some((ref cert_file, ref key_file)) = config.cert_key_file {
            try!(ssl_ctx.set_certificate_file(cert_file.as_str(), X509FileType::PEM));
            try!(ssl_ctx.set_private_key_file(key_file.as_str(), X509FileType::PEM));
        }
        ssl_ctx.set_verify(ssl::SSL_VERIFY_NONE, None);
        let ssl_connector = HttpsConnector::new(OpensslClient::new(ssl_ctx));
        let http_client = HttpClient::with_connector(ssl_connector);
        Ok(XBusClient {
            config: config,
            client: http_client,
        })
    }

    fn request<'a>(&'a self, method: Method, path: &'a str) -> RequestBuilder<'a> {
        RequestBuilder::new(&self.client, &self.config.endpoint, method, path)
    }

    pub fn get(&self, key: &str) -> Result<Item, Error> {
        self.request(Method::Get, &format!("/api/configs/{}", key))
            .send()
            .map(|r: ItemResult| r.config)
    }
}

#[derive(Deserialize)]
pub struct ItemResult {
    config: Item,
    revision: u64,
}

#[derive(Deserialize, Debug)]
pub struct Item {
    pub name: String,
    pub value: String,
    pub version: u64,
}

#[cfg(test)]
mod test {
    use super::XBusClient;
    use super::Config;
    use super::Item;

    fn client() -> XBusClient {
        let config = Config {
            endpoint: "https://localhost:4433".to_owned(),
            ca_file: Some("cacert.pem".to_owned()),
            cert_key_file: None,
        };
        XBusClient::new(config).unwrap()
    }

    #[test]
    fn test_get() {
        let cli = client();
        let item = cli.get("fms.testkey").unwrap();
        println!("item: {:?}", item);
    }
}
