; COMMAND-LINE: --incremental
; EXPECT: sat
; EXPECT: sat
(set-option :incremental true)
(set-logic QF_LIA)
(declare-fun x () Int)
(check-sat)
(define t (not (= x 0)))
(assert t)
(check-sat)
