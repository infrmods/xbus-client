#![feature(proc_macro)]

extern crate openssl;
extern crate serde;
#[macro_use]
extern crate serde_derive;
extern crate serde_yaml;
extern crate serde_json;
extern crate hyper;
extern crate url;

mod error;
mod client;
mod request;

#[cfg(test)]
mod tests {
    #[test]
    fn it_works() {}
}
