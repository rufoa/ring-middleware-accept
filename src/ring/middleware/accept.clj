(ns ring.middleware.accept)

(defn- max-key-multi
	"like max-key but with tie-breaking"
	[cands & score-fns]
	(when-not (empty? cands)
		(let [score (apply juxt score-fns)]
			(reduce #(if (pos? (compare (score %1) (score %2))) %1 %2) cands))))

(defn- max-pos-key-multi
	"like max-key-multi but disqualify any candidates that give negative scores"
	[cands & score-fns]
	(let [score (apply juxt score-fns)]
		(apply max-key-multi
			(filter #(every? pos? (score %)) cands)
			score-fns)))

(defn- match
	[offered prefs match-fn]
	(let [most-applicable-rule
			(fn [input]
				(max-pos-key-multi prefs #(match-fn input (:name %))))]
		(if (seq offered)
			(let
				[result (max-pos-key-multi offered
					#(if-let [rule (most-applicable-rule (:name %))] (* (:qs % 1) (:q rule 0)) -1)
					#(if-let [rule (most-applicable-rule (:name %))] (match-fn (:name %) (:name rule)) -1))]
				(or (:as result) (:name result))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- exact-match
	[cand pattern]
	(if (or (= cand pattern) (= pattern "*")) 1 0))

(defn- mime-match
	[cand pattern]
	(reduce
		(fn [s [c p]] (cond (= p "*") s (= c p) (* s 2) :else 0)) ; award points for exact match but not * match
		1
		(map vector
			(clojure.string/split cand    #"/" 2)
			(clojure.string/split pattern #"/" 2))))

(defn- lang-match
	[cand pattern]
	(let
		[cand (.toLowerCase cand)
		 pattern (.toLowerCase pattern)
		 cand-len (count cand) pattern-len (count pattern)]
		(if
			(or (= cand pattern)
				(= pattern "*")
				(and (> cand-len pattern-len)
					(= (str pattern "-") (subs cand 0 (+ pattern-len 1)))))
			pattern-len 0))) ; prefer closer match

(defn- charset-post
	"If no * is present in an Accept-Charset field, then [...] ISO-8859-1 [...] gets a quality value of 1 if not explicitly mentioned.
	http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.2"
	[prefs]
	(if (and
			(not-any? #(= (:name %) "*") prefs)
			(not-any? #(= (:name %) "iso-8859-1") prefs))
		(conj prefs {:name "iso-8859-1" :q 1})
		prefs))

(defn- encoding-post
	"The identity content-coding is always acceptable, unless specifically refused
	http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.3"
	[prefs]
	(if (and
			(not-any? #(= (:name %) "*") prefs)
			(not-any? #(= (:name %) "identity") prefs))
		(conj prefs {:name "identity" :q 0.0009}) ; lower than any other encodings which were actually given (0.001 is the lowest permitted)
		prefs))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- parse-accepts
	"Parse client capabilities and associated q-values"
	[accepts-string]
	(map
		(fn [[_ name q]]
			{:name name :q (Float/parseFloat (or q "1"))})
		(re-seq #"([^,;\s]+)[^,]*?(?:;\s*q=(0(?:\.\d{0,3})?|1(?:\.0{0,3})?))?" accepts-string)))

(defn- parse-offered
	"Parse offered types and associated source-qualities and aliases"
	[offered-list]
	(loop [res [] cur {} [a b :as unprocessed] offered-list]
		(cond
			(empty? unprocessed)
				(if (empty? cur) res (conj res cur))
			(some (partial = a) [:as :qs])
				(recur res (assoc cur a b) (drop 2 unprocessed))
			:else
				(recur (if (empty? cur) res (conj res cur)) {:name a} (rest unprocessed)))))

(defn wrap-accept
	[handler {:keys [mime charset encoding language]}]
	(let [match*
			(fn [offered accepts matcher-fn post-fn]
				(match (parse-offered offered) (post-fn (parse-accepts accepts)) matcher-fn))
			assoc-in-once
			#(if (nil? (get-in %1 %2)) (assoc-in %1 %2 %3) %1)]
		(fn [{headers :headers :as request}]
			(-> request
				(assoc-in-once [:accept :mime]     (match* mime     (headers "accept" "*/*")               mime-match  identity))
				(assoc-in-once [:accept :charset]  (match* charset  (headers "accept-charset" "*")         exact-match charset-post))
				(assoc-in-once [:accept :encoding] (match* encoding (headers "accept-encoding" "identity") exact-match encoding-post))
				(assoc-in-once [:accept :language] (match* language (headers "accept-language" "*")        lang-match  identity))
				(handler)))))