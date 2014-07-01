ring-middleware-accept
======================

ring-middleware-accept is a Ring middleware component for performing **server-driven content negotiation**.

It allows Ring handlers to select the most appropriate content to output based on the value of these headers in the incoming request:

* Accept
* Accept-Language
* Accept-Charset
* Accept-Encoding

It implements [RFC 2616 sections 14.1–4](http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1), including **wildcard and prefix matching** rules, client-side **q-values**, and server-side **source quality (qs) values**.

[![Build Status](https://travis-ci.org/rufoa/ring-middleware-accept.png?branch=master)](https://travis-ci.org/rufoa/ring-middleware-accept)

## Installation ##

ring-middleware-accept is available in [Clojars](https://clojars.org/ring-middleware-accept). Add it as a dependency in your Leiningen project's `project.clj`:

```clojure
[ring-middleware-accept "1.0.1"]
```

or to your Maven project's `pom.xml`:

```xml
<dependency>
	<groupId>ring-middleware-accept</groupId>
	<artifactId>ring-middleware-accept</artifactId>
	<version>1.0.1</version>
</dependency>
```

## Use ##

ring-middleware-accept exposes a single public function, `wrap-accept`. It takes two arguments: the handler to be wrapped, and a map of the content types offered by the handler.

For example:

```clojure
(wrap-accept my-handler
	{:mime ["text/html" "text/plain"], :language ["en" "fr" "de"]})
```

Valid keys in the map are `:mime` (corresponding to the `Accept` header), `:language`, `:charset`, and `:encoding`. In the simplest case, the map values are just vectors of strings.

The wrapper augments the request-map which is passed to the handler with an `:accept` entry. Its value is a map of results, i.e. which of the offered content types the client should be served.

For example:

```clojure
{:mime "text/html", :language "en"}
```

If the client cannot accept any of the offered types, some of the map entries will be `nil`. This may warrant a `406 Not Acceptable` HTTP response.

### Simple example ###

```clojure
(defroutes app
	(wrap-accept
		(GET "/" {accept :accept}
			(case (:language accept)
				"en" "hello"
				"fr" "bonjour"
				"de" "hallo"))
		{:language ["en" "fr" "de"]}))
```

### TODO: further examples illustrating aliases, wildcards, prefix-matching, q-values, qs-values ###

## License ##

Copyright © 2013 [rufoa](https://github.com/rufoa)

Distributed under the Eclipse Public License, the same as Clojure.