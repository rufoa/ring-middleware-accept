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
[ring-middleware-accept "2.0.3"]
```

or to your Maven project's `pom.xml`:

```xml
<dependency>
	<groupId>ring-middleware-accept</groupId>
	<artifactId>ring-middleware-accept</artifactId>
	<version>2.0.3</version>
</dependency>
```

## Use ##

ring-middleware-accept exposes a single public function, `wrap-accept`, in the namespace `ring.middleware.accept`. This function takes two arguments: the handler to be wrapped, and a map of the content types offered by the handler.

For example:

```clojure
(wrap-accept my-handler
	{:mime ["text/html" "text/plain"], :language ["en" "fr" "de"]})
```

Valid keys in the map are `:mime` (corresponding to the `Accept` header), `:language`, `:charset`, and `:encoding`. In the simplest case, as in the example above, the map values are just vectors of strings.

The wrapper augments the request-map which is passed to the handler with an `:accept` entry. Its value is a map of results, i.e. which of the offered content types the client should be served.

For example:

```clojure
{:mime "text/html", :language "en"}
```

If the client cannot accept any of the offered types, some of the map entries will be `nil`. This may warrant a `406 Not Acceptable` HTTP response.

### Simple example ###

```clojure
(ns example.web
	(:require [ring.middleware.accept :refer [wrap-accept]])
	;...
	)

(defroutes routes
	(GET "/greeting" {accept :accept}
		(case (:language accept)
			"en" "hello"
			"fr" "bonjour"
			"de" "hallo")))

(def app
	(-> routes
		(wrap-accept {:language ["en" "fr" "de"]})
		))
```

### Aliases ###

Content types can be given aliases using the `:as` keyword.

```clojure
{:language ["en-gb" :as :british, "en-us" :as :american]
 :mime     ["application/json" :as :json, "text/html" :as :html]}
```

These aliases will then used in the map of results:

```clojure
{:language :british, :mime :html}
```

### Source quality (qs) values ###

The `:qs` keyword can be used to assign quality values to the content types which the server is offering. These are the server-side equivalent of the q-values found in client requests.

This example shows a server expressing a preference for HTML over plain text in the ratio 2:1.

```clojure
{:mime ["text/html" :qs 1, "text/plain" :qs 0.5]}
```

These values are used to determine which content type is chosen in the event of the client being able to accept more than one of those on offer. ring-middleware-accept follows the de facto standard of multiplying client q-values and server qs-values, and selecting the greatest product.

This means that in our example, a client which accepts `text/plain,text/html` will be served HTML, but a client which accepts `text/plain;q=1,text/html;q=0.1` will be served plain text. This is because the `text/plain` product 1×0.5 is greater than the `text/html` product 0.1×1.

## License ##

Copyright © 2014 [rufoa](https://github.com/rufoa)

Distributed under the Eclipse Public License, the same as Clojure.