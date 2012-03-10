(ns ansuz.extra
  (:refer-clojure :exclude [reify])
  (:use [ansuz.reflect])
  (:use [ansuz.monad])
  (:use [ansuz.core :only [!]])
  (:use [ansuz.monadplus])
  (:use [ansuz.language]))

;; (defmacrop kleene [m]
;;   (let[mm (gensym 'mm)
;;        kl (gensym 'kl)
;;        kl1 (gensym 'kl)
;;        es (gensym 'es)
;;        e  (gensym 'e)]
;;     `(reify (~mm ~m)
;;                   (parser-eval
;;                    (:let [~kl1 (parser ~kl [~es]
;;                                        (:or (:do (:= ~e (~mm)) (~kl (conj ~es ~e)))
;;                                             (ret ~es)))]
;;                          (~kl1 []))))))

;; (defmacrop ** [m]
;;   (let[mm (gensym 'mm)
;;        kl (gensym 'kl)
;;        kl1 (gensym 'kl)
;;        es (gensym 'es)
;;        e  (gensym 'e)]
;;     `(reify (~mm ~m)
;;                   (let [~kl (parser ~kl [~es]
;;                                           (alt
;;                                            ((do ((:= ~e (~mm)) (~kl (conj ~es ~e))))
;;                                             (ret ~es))))]
;;                              (~kl [])))))


;; (defmacrop ?? [m]
;;   `(reify (~mm (parser-eval ~m))
;;                 (orelse (parser-eval ~mm) (ret false))))

(defmacrop maybe [m]
  (let[mm (gensym 'm)]
  `(reify (~mm (evalp ~m))
     (evalp (~'alt (~mm) (ret false))))))

(defmacrop many [m]
  (let[mm (gensym 'mm)
       kl (gensym 'kl)
       kl1 (gensym 'kl)
       es (gensym 'es)
       e  (gensym 'e)]
    `(reify [~mm (evalp ~m)]
       (evalp
         (~'let [~kl (parser ~kl [~es]
                             (~'alt (~'cat (~'<- ~e (~mm))
                                           (~kl (conj ~es ~e)))
                                    (ret ~es)))]
           (~kl []))))))

(defmacrop up [n m]
  (let[mm (gensym 'mm)
       ut (gensym 'ut)
       e  (gensym 'e)
       es (gensym 'es)
       j  (gensym 'j)]
    `(reify (~mm (evalp ~m))
       (evalp
         (~'let [~ut (parser ~ut [~j ~es]
                             (~'if (= j 0) (ret ~es)
                                   (~'alt (~'cat (~'<- ~e (~mm))
                                                 (~ut (- ~j 1) (ocnj ~es ~e)))
                                          (ret ~es))))])))))

(defmacrop stringp [s]
  `(evalp (~'cat ~@(map (fn [x] `(! ~x)) s))))

