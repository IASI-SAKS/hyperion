:- set_prolog_flag(character_escapes,false).

% invokes/9 semantics
% invokes(
% 1 test name,
% 2 branch point,
% 3 branch sequence number,
% 4 caller,
% 5 callerProgramCounter,
% 6 frameEpoch,
% 7 path condition,
% 8 callee,
% 9 parameters)

%% SIMILARITY RELATION ---------------------------------------------------------
% MODE: sub_set_of_invoked_methods(+TP1,+TP2)
% SEMANTICS: sub_set_of_invoked_methods(+TP1,+TP2) holds if all methods invoked
% by TP1 are invoked by TP2.
sub_set_of_invoked_methods(TP1,TP2) :-
  exists_method_not_invoked(TP1,TP2),
  !,
  fail.
sub_set_of_invoked_methods(_TP1,_TP2).

% MODE: exists_method_not_invoked(+TP1,+TP2)
% SEMANTICS: there exists a method M invoked by TP1 that is not invoked by TP2
exists_method_not_invoked(TP1,TP2) :-
  tp_invokes_m(TP1,M),
  \+ tp_invokes_m(TP2,M).

% MODE: tp_invokes_m(?TP,?M)
% SEMANTICS: TP invokes M
tp_invokes_m(TP,M) :-
  invokes(TP,_,_,_,_,_,_,M,_).

%% SIMILARITY RELATION ---------------------------------------------------------
% MODE: eq_set_of_invoked_methods(+TP1,+TP2)
% SEMANTICS: eq_set_of_invoked_methods(+TP1,+TP2) holds if TP1 and TP2 invoke
% the same set of methods.
eq_set_of_invoked_methods(TP1,TP2) :-
  sub_set_of_invoked_methods(TP1,TP2),
  sub_set_of_invoked_methods(TP2,TP1).

%% SIMILARITY RELATION ---------------------------------------------------------
% MODE: sub_set_of_maximalInvokeSequences(+TP1,+TP2,+Caller)
% SEMANTICS: sub_set_of_maximalInvokeSequences(TP1,TP2,Caller) holds if
% the set of maximal sequences of direct invocations performed by Caller in TP1
% is a subset of those performed by Caller in TP2
% (library(ordsets): https://www.swi-prolog.org/pldoc/man?section=ordsets)
sub_set_of_maximalInvokeSequences(TP1,TP2,Caller) :-
  findall(MSeq, (maximalInvokeSequence(TP1,Caller,ISeq), invokes_callees(ISeq,MSeq)), MSeq1Lst),
  list_to_ord_set(MSeq1Lst,S1), % library(ordsets) predicate
  ( ord_empty(S1) ->
    true
  ; (
      findall(MSeq, (maximalInvokeSequence(TP2,Caller,ISeq), invokes_callees(ISeq,MSeq)), MSeq2Lst),
      list_to_ord_set(MSeq2Lst,S2),
      ord_subset(S1,S2)          % library(ordsets) predicate
    )
  ).

% MODE: eq_set_of_maximalInvokeSequences(+TP1,+TP2,+Caller)
% SEMANTICS: eq_set_of_maximalInvokeSequences(TP1,TP2,Caller) holds if
% the set of maximal sequences of direct invocations performed by Caller in TP1 and
% the set of maximal sequences of direct invocations performed by Caller in TP2
% have the same elements
eq_set_of_maximalInvokeSequences(TP1,TP2,Caller) :-
  findall(MSeq, (maximalInvokeSequence(TP1,Caller,ISeq), invokes_callees(ISeq,MSeq)), MSeq1Lst),
  list_to_ord_set(MSeq1Lst,S1),
  findall(MSeq, (maximalInvokeSequence(TP2,Caller,ISeq), invokes_callees(ISeq,MSeq)), MSeq2Lst),
  list_to_ord_set(MSeq2Lst,S2),
  ord_seteq(S1,S2).             % library(ordsets) predicate

% SEMANTICS: maximalInvokeSequence(TP,Caller,ISeq)
% ISeq is a maximal sequence of direct invocations performed by Caller in TP
maximalInvokeSequence(TP,Caller,[FirstInvokes|ISeqTail]) :-
  first_invokes(TP,Caller,FirstInvokes),
  last_invokes(TP,Caller,LastInvokes),
  iseq(FirstInvokes,[FirstInvokes|ISeqTail]),
  last(ISeqTail,LastInvokes).

% Invokes is the first invocation performed by Caller in TP
first_invokes(TP,Caller,Invokes) :-
  Invokes = invokes(TP,_,_,Caller,_,_,_,_,_), Invokes,
  \+ exists_preceeding_invokes(Invokes).
% first_invokes utility predicate
exists_preceeding_invokes(Invokes) :- next(_,Invokes).
% Invokes is the last invocation performed by Caller in TP
last_invokes(TP,Caller,Invokes) :-
  Invokes = invokes(TP,_,_,Caller,_,_,_,_,_), Invokes,
  \+ exists_succeeding_invokes(Invokes).
% last_invokes utility predicate
exists_succeeding_invokes(Invokes) :- next(Invokes,_).

% SEMANTICS: iseq(I,ISeq)
% ISeq is a sequence of invocations performed by Caller in TP starting from I
iseq(I1,[I1|Is]) :-
  I1 = invokes(TP,_,_,Caller,_,_,_,_,_),
  I2 = invokes(TP,_,_,Caller,_,_,_,_,_),
  next(I1,I2),
  iseq(I2,Is).
iseq(I1,[I1]).

% SEMANTICS: next(Invokes1,Invokes2)
% Invokes2 is a subsequent invocation performed by Caller in TP
next(Invokes1,Invokes2) :-
  Invokes1 = invokes(TP,BP,SN1,Caller,_,_,_,_,_), Invokes1,
  Invokes2 = invokes(TP,BP,SN2,Caller,_,_,_,_,_), Invokes2,
  SN2 > SN1,
  \+ exists_intermediate_invokes(Invokes1,Invokes2),
  !.
next(Invokes1,Invokes2) :-
  Invokes1 = invokes(TP,BP1,_,Caller,_,_,_,_,_), Invokes1,
  Invokes2 = invokes(TP,BP2,_,Caller,_,_,_,_,_), Invokes2,
  append(BP1,[_|_],BP2),
  \+ exists_intermediate_invokes(Invokes1,Invokes2).

% SEMANTICS: exists_intermediate_invokes(Invokes1,Invokes2) holds if
% there exists an invokes InvokesM having the same TP and Caller of
% Invokes1 and Invokes2, and
% either (a.1) it has the same branching point BP of Invokes1 and Invokes2, and
% (a.2) its seq.no. SN is greater than the seq.no SN1 of Invokes1 and
% less than the seq.no. SN2 of Invokes2
exists_intermediate_invokes(Invokes1,Invokes2) :-
  Invokes1 = invokes(TP,BP,SN1,Caller,_,_,_,_,_),
  Invokes2 = invokes(TP,BP,SN2,Caller,_,_,_,_,_),
  InvokesM = invokes(TP,BP, SN,Caller,_,_,_,_,_), InvokesM, % (a.1)
  SN1 < SN, SN < SN2,                                       % (a.2)
  !.
% or (b.1) its branching point BP extends the branching point BP1 of Invokes1,
% and (b.2) BP is a prefix of the branching point BP2 of Invokes2, and either
% (b.2.1) BP2 extends BP, or
% (b.2.2) BP2 = BP and a seq.no. SN less than the seq.no. SN2 of Invokes2
exists_intermediate_invokes(Invokes1,Invokes2) :-
  Invokes1 = invokes(TP,BP1,  _,Caller,_,_,_,_,_),
  Invokes2 = invokes(TP,BP2,SN2,Caller,_,_,_,_,_),
  BP1 \= BP2,
  InvokesM = invokes(TP,BP,  SN,Caller,_,_,_,_,_), InvokesM,
  append(BP1,[_|_],BP),      % (b.1)
  append(BP,Infix,BP2),      % (b.2)
  ( Infix = [_|_]            % (b.2.1)
  ;
    ( Infix = [], SN < SN2 ) % (b.2.2)
  ).

% SEMANTICS: invokeSequence(TP,Caller,ISeq)
% ISeq is a sequence of direct invocations performed by Caller in TP
invokeSequence(TP,Caller,ISeq) :-
  maximalInvokeSequence(TP,Caller,MSeq),
  ISeq=[_,_|_], % subsequences with at least two methods
  prefix(ISeq,MSeq).

% Utility predicates -----------------------------------------------------------
% MODE: testPrograms(-TPs)
% SEMANTICS: TPs is the set of executed Test Programs
testPrograms(TPs) :-
  setof(TP, [B,C,D,E,F,G,H,I]^invokes(TP,B,C,D,E,F,G,H,I), TPs).

% MODE: callers(-Cs)
% SEMANTICS: Cs is the set of callers occurring in the symbolic execution
callers(Cs) :-
  setof(Caller, [A,B,C,E,F,G,H,I]^invokes(A,B,C,Caller,E,F,G,H,I), Cs).

% MODE: tp_callers(+TP,-Cs)
% SEMANTICS: Cs is the set of callers occurring in the symbolic execution of TP
tp_callers(TP,Cs) :-
  setof(Caller, [B,C,E,F,G,H,I]^invokes(TP,B,C,Caller,E,F,G,H,I), Cs).

% MODE: invokes_callers(+Is,-Cs)
% SEMANTICS: Cs is the list of callers occurring in the list of invokes Is
invokes_callers([],[]).
invokes_callers([I|Is],[C|Cs]) :-
  I = invokes(_,_,_,C,_,_,_,_,_),
  invokes_callers(Is,Cs).

% MODE: callees(?Cs)
% SEMANTICS: Cs is the set of callees occurring in the symbolic execution
callees(Cs) :-
  setof(Callee, [A,B,C,D,E,F,G,I]^invokes(A,B,C,D,E,F,G,Callee,I), Cs).

% MODE: tp_callees(+TP,-Cs)
% SEMANTICS: Cs is the set of callees occurring in the symbolic execution of TP
tp_callees(TP,Cs) :-
  setof(Callee, [B,C,D,E,F,G,I]^invokes(TP,B,C,D,E,F,G,Callee,I), Cs).

% MODE: invokes_callees(+Is,-Cs)
% SEMANTICS: Cs is the list of callees occurring in the list of invokes Is
invokes_callees([],[]).
invokes_callees([I|Is],[C|Cs]) :-
  I = invokes(_,_,_,_,_,_,_,C,_),
  invokes_callees(Is,Cs).

% SEMANTICS: mseq_invokes(Ms,Is) holds if Ms is a list of the form [M1,...,Mk],
% and I is a list of invokes of length k s.t. for i=1,...,k Mi is the callee
% method of Ii, and I can be obtained by deleting zero or more invokes from Is.
mseq_invokes([],_).
mseq_invokes([M|Ms],[I|Is]) :-
  I = invokes(_,_,_,_,_,_,_,M,_),
  mseq_invokes(Ms,Is).
mseq_invokes([M|Ms],[_|Is]) :-
  mseq_invokes([M|Ms],Is).

% SEMANTICS: mset_invokes(Ms,Is) holds if Ms is a list of the form [M1,...,Mk],
% and I is a list of invokes of length k s.t. for i=1,...,k Mi is the callee
% method of Ii and Ii is a member of Is.
% ASSUMPTION: Ms is a list of distinct elements.
mset_invokes([],_).
mset_invokes([M|Ms],Is) :-
  I = invokes(_,_,_,_,_,_,_,M,_),
  selectchk(I,Is,Is1),
  mset_invokes(Ms,Is1).

% MODE: filter(+Xs,+XSchema,+Filter,+XArgs,+YName, -Ys)
% NOTATION: Given a list L, we denote by L[i] the i-th element of L.
% SEMANTICS: filter(Xs,XSchema,Filter,XArgs,YName, Ys) holds if
% Xs is a list of atoms XName/XArity
% XSchema is a ground term with functor XName/XArity
% Filter is a list of triples of the form (Pred,ArgP,Pars), where:
%   Pred is a user defined predicate
%   ArgP is an integer between 1 and XArity
%   Pars is a list of parameters
% XArgs is a list of integers between 1 and XArity
% YName is a functor name
% Ys is a list of atoms of the form YName(A1,...,An) s.t.
% there exists an atom X in Xs satisfying the following conditions:
%   - for all i in 1,...,n. Ai is the argument of X at position XArgs[i]
%   - for all (Pred,ArgP,Pars) in Filter.
%       Par is the argument of X at position ArgP and Pred(Par,Pars) holds
filter([],_XSchema,_Filter,_XArgs,_YName, []).
filter([X|Xs],XSchema,Filter,XArgs,YName, [Y|Ys]) :-
  satisfy(X,XSchema,Filter),
  !,
  eval_proj_func(XArgs,XSchema,X, YArgs),
  Y =.. [YName|YArgs],
  filter(Xs,XSchema,Filter,XArgs,YName, Ys).
filter([_|Xs],XSchema,Filter,XArgs,YName, Ys) :-
  filter(Xs,XSchema,Filter,XArgs,YName, Ys).

% MODE: satisfy(+X,+Ps)
% SEMANTICS: satisfy(X,Ps) holds if for all (Pred,ArgP,Pars) in Ps.
% Par is the argument of X at position ArgP and Pred(Par,Pars) holds
satisfy(_X,_XSchema,[]).
satisfy(X,XSchema,[P|Ps]) :-
  P =.. [Pred,Name|Pars],
  arg(I,XSchema,Name),
  arg(I,X,XVal),
  F =.. [Pred,XVal|Pars],
  call(F),
  satisfy(X,XSchema,Ps).

% MODE: eval_proj_func(+F,+XSchema,+X, -XVals)
% SEMANTICS: XVals is the result of applying F to X.
eval_proj_func(F,XSchema,X, XVal) :-
  arg(I,XSchema,F),
  !,
  arg(I,X,XVal).
eval_proj_func([],_XSchema,_X, []).
eval_proj_func([F|Fs],XSchema,X, [XVal|XVals]) :-
  eval_proj_func(F,XSchema,X, XVal),
  eval_proj_func(Fs,XSchema,X, XVals).
eval_proj_func(F,XSchema,X, XVal) :-
  \+ is_list(F),
  F =.. [P|As],
  eval_proj_func(As,XSchema,X, Es),
  G =.. [P|Es],
  call(G,XVal).


% user defined predicates to be used in filter/6
subStr(A,S) :-
  % (https://www.swi-prolog.org/pldoc/doc_for?object=sub_atom/5)
  sub_atom(A,_,_,_,S).

nthSubStr(L,I,S) :-
  nth1(I,L,E),
  subStr(E,S).

head([H|_],H).

httpMethod(A,H) :-
  member(H,['get','post','put','delete']),
  atom_concat('MockMvcRequestBuilders:',H,M),
  sub_atom(A,_,_,_,M).

javaMethod(A,M) :-
  atom_chars(A,C),
  append(_,[:|L],C),
  !,
  atom_chars(M,L).
