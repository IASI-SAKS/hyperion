:- set_prolog_flag(character_escapes,false).

% invokes/9 semantics
% invokes(
% 1 testProgram,
% 2 branchPoint,
% 3 branchSequenceNumber,
% 4 caller,
% 5 callerProgramCounter,
% 6 frameEpoch,
% 7 pathCondition,
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
  first_miseq_invokes(TP,Caller,FirstInvokes),
  last_miseq_invokes(TP,Caller,LastInvokes),
  miseq_invokes(FirstInvokes,[FirstInvokes|ISeqTail]),
  last(ISeqTail,LastInvokes).

% Invokes is the first invocation performed by Caller in TP
first_miseq_invokes(TP,Caller,Invokes) :-
  Invokes = invokes(TP,_,_,Caller,_,_,_,_,_), Invokes,
  \+ exists_miseq_preceeding_invokes(Invokes).
% first_invokes utility predicate
exists_miseq_preceeding_invokes(Invokes) :- miseq_next(_,Invokes).
% Invokes is the last invocation performed by Caller in TP
last_miseq_invokes(TP,Caller,Invokes) :-
  Invokes = invokes(TP,_,_,Caller,_,_,_,_,_), Invokes,
  \+ exists_miseq_succeeding_invokes(Invokes).
% last_invokes utility predicate
exists_miseq_succeeding_invokes(Invokes) :- miseq_next(Invokes,_).

% SEMANTICS: miseq_invokes(I,ISeq)
% ISeq is a sequence of invocations performed by Caller in TP starting from I
miseq_invokes(I1,[I1|Is]) :-
  I1 = invokes(TP,_,_,Caller,_,_,_,_,_),
  I2 = invokes(TP,_,_,Caller,_,_,_,_,_),
  miseq_next(I1,I2),
  miseq_invokes(I2,Is).
miseq_invokes(I1,[I1]).

% SEMANTICS: miseq_next(Invokes1,Invokes2)
% Invokes2 is a subsequent invocation performed by Caller in TP
miseq_next(Invokes1,Invokes2) :-
  Invokes1 = invokes(TP,BP,SN1,Caller,_,_,_,_,_), Invokes1,
  Invokes2 = invokes(TP,BP,SN2,Caller,_,_,_,_,_), Invokes2,
  SN2 > SN1,
  \+ exists_miseq_intermediate_invokes(Invokes1,Invokes2),
  !.
miseq_next(Invokes1,Invokes2) :-
  Invokes1 = invokes(TP,BP1,_,Caller,_,_,_,_,_), Invokes1,
  Invokes2 = invokes(TP,BP2,_,Caller,_,_,_,_,_), Invokes2,
  append(BP1,[_|_],BP2),
  \+ exists_miseq_intermediate_invokes(Invokes1,Invokes2).

% SEMANTICS: exists_miseq_intermediate_invokes(Invokes1,Invokes2) holds if
% there exists an invokes InvokesM having the same TP and Caller of
% Invokes1 and Invokes2, and
% either (a.1) it has the same branching point BP of Invokes1 and Invokes2, and
% (a.2) its seq.no. SN is greater than the seq.no SN1 of Invokes1 and
% less than the seq.no. SN2 of Invokes2
exists_miseq_intermediate_invokes(Invokes1,Invokes2) :-
  Invokes1 = invokes(TP,BP,SN1,Caller,_,_,_,_,_),
  Invokes2 = invokes(TP,BP,SN2,Caller,_,_,_,_,_),
  InvokesM = invokes(TP,BP, SN,Caller,_,_,_,_,_), InvokesM, % (a.1)
  SN1 < SN, SN < SN2,                                       % (a.2)
  !.
% or (b.1) its branching point BP extends the branching point BP1 of Invokes1,
% and (b.2) BP is a prefix of the branching point BP2 of Invokes2, and either
% (b.2.1) BP2 extends BP, or
% (b.2.2) BP2 = BP and a seq.no. SN less than the seq.no. SN2 of Invokes2
exists_miseq_intermediate_invokes(Invokes1,Invokes2) :-
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

% SEMANTICS: trace(TP,Trace)
% Trace is a trace generated by the symbolic execution of TP
trace(TP,Trace) :-
  testProgram_entry_point_invokes(TP,Invokes),
  trace_invokes(Invokes,Trace).

% SEMANTICS: trace_invokes(I,IS)
% IS is a sequence of invokes starting from I
trace_invokes(I1,[I1|Is]) :-
  invokes_component(I1,testProgram,TP),
  invokes_component(I2,testProgram,TP),
  trace_next(I1,I2),
  trace_invokes(I2,Is).
trace_invokes(I1,[I1]) :-
  \+ exists_trace_succeeding_invokes(I1).

% trace utility predicates
exists_trace_preceeding_invokes(Invokes) :- miseq_next(_,Invokes).
exists_trace_succeeding_invokes(Invokes) :- trace_next(Invokes,_).

% SEMANTICS: trace_next(Invokes1,Invokes2)
% Invokes2 is a subsequent invocation performed in TP
trace_next(Invokes1,Invokes2) :-
  Invokes1 = invokes(TP,BP,SN1,_,_,_,_,_,_), Invokes1,
  Invokes2 = invokes(TP,BP,SN2,_,_,_,_,_,_), Invokes2,
  SN2 > SN1,
  \+ exists_trace_intermediate_invokes(Invokes1,Invokes2),
  !.
trace_next(Invokes1,Invokes2) :-
  Invokes1 = invokes(TP,BP1,_,_,_,_,_,_,_), Invokes1,
  Invokes2 = invokes(TP,BP2,_,_,_,_,_,_,_), Invokes2,
  append(BP1,[_|_],BP2),
  \+ exists_trace_intermediate_invokes(Invokes1,Invokes2).

% SEMANTICS: exists_trace_intermediate_invokes(Invokes1,Invokes2) holds if
% there exists an invokes InvokesM having the same TP and Caller of
% Invokes1 and Invokes2, and
% either (a.1) it has the same branching point BP of Invokes1 and Invokes2, and
% (a.2) its seq.no. SN is greater than the seq.no SN1 of Invokes1 and
% less than the seq.no. SN2 of Invokes2
exists_trace_intermediate_invokes(Invokes1,Invokes2) :-
  Invokes1 = invokes(TP,BP,SN1,_,_,_,_,_,_),
  Invokes2 = invokes(TP,BP,SN2,_,_,_,_,_,_),
  InvokesM = invokes(TP,BP, SN,_,_,_,_,_,_), InvokesM, % (a.1)
  SN1 < SN, SN < SN2,                                  % (a.2)
  !.
% or (b.1) its branching point BP extends the branching point BP1 of Invokes1,
% and (b.2) BP is a prefix of the branching point BP2 of Invokes2, and either
% (b.2.1) BP2 extends BP, or
% (b.2.2) BP2 = BP and a seq.no. SN less than the seq.no. SN2 of Invokes2
exists_trace_intermediate_invokes(Invokes1,Invokes2) :-
  Invokes1 = invokes(TP,BP1,  _,_,_,_,_,_,_),
  Invokes2 = invokes(TP,BP2,SN2,_,_,_,_,_,_),
  BP1 \= BP2,
  InvokesM = invokes(TP,BP,  SN,_,_,_,_,_,_), InvokesM,
  append(BP1,[_|_],BP),      % (b.1)
  append(BP,Infix,BP2),      % (b.2)
  ( Infix = [_|_]            % (b.2.1)
  ;
    ( Infix = [], SN < SN2 ) % (b.2.2)
  ).

% MODE: testProgram_entry_point_invokes(+TP,-Invokes)
% SEMANTICS: Invokes is the first invocation performed by TP
testProgram_entry_point_invokes(TP,Invokes) :-
  testProgram_entry_point_caller(TP,Caller),
  Invokes = invokes(TP,_,_,Caller,_,_,_,_,_), Invokes, % required for \+
  \+ exists_trace_preceeding_invokes(Invokes).
% MODE: testProgram_entry_point_caller(+TP,-Caller)
% SEMANTICS: Caller is the caller method occurring in the
% first invocation performed by TP
testProgram_entry_point_caller(TP,Caller) :-
  atom_chars(TP,TPChars),
  replace(TPChars,'.','/',CallerPrefixChars),
  atom_chars(CallerPrefix,CallerPrefixChars),
  atom_concat(CallerPrefix,':()V',Caller).

% Utility predicates -----------------------------------------------------------

% utility predicates to get the components of invokes
invokes_component(I,testProgram,TestProgram) :-
  I = invokes(TestProgram,_,_,_,_,_,_,_,_).
invokes_component(I,branchPoint,BranchPoint) :-
  I = invokes(_,BranchPoint,_,_,_,_,_,_,_).
invokes_component(I,branchSequenceNumber,BranchSequenceNumber) :-
  I = invokes(_,_,BranchSequenceNumber,_,_,_,_,_,_).
invokes_component(I,caller,Caller) :-
  I = invokes(_,_,_,Caller,_,_,_,_,_).
invokes_component(I,callerProgramCounter,CallerProgramCounter) :-
  I = invokes(_,_,_,_,CallerProgramCounter,_,_,_,_).
invokes_component(I,frameEpoch,FrameEpoch) :-
  I = invokes(_,_,_,_,_,FrameEpoch,_,_,_).
invokes_component(I,pathCondition,PathCondition) :-
  I = invokes(_,_,_,_,_,_,PathCondition,_,_).
invokes_component(I,callee,Callee) :-
  I = invokes(_,_,_,_,_,_,_,Callee,_).
invokes_component(I,parameters,Parameters) :-
  I = invokes(_,_,_,_,_,_,_,_,Parameters).

% MODE: replace(+CharsIn,+X,+Y,-CharsOut)
% SEMANTICS: CharsOut is obtained from CharsIn by replacing
% all occurrences of X by Y
replace([],_X,_Y,[]).
replace([CharIn|CharsIn],X,Y,[CharOut|CharsOut]) :-
  CharIn == X,
  !,
  CharOut = Y,
  replace(CharsIn,X,Y,CharsOut).
replace([CharIn|CharsIn],X,Y,[CharOut|CharsOut]) :-
  CharIn = CharOut,
  replace(CharsIn,X,Y,CharsOut).

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

% MODE: filter(+Xs,+XSchema,+XSelectFuns,+XArgsSelect,+YName, -Ys)
% NOTATION: Given a list L, we denote by L[i] the i-th element of L.
% SEMANTICS: filter(Xs,XSchema,Filter,XArgs,YName, Ys) holds if
% Xs is a list of atoms XName/XArity
% XSchema is a ground term with functor XName/XArity
% XSelectFuns is a list of (user defined) boolean functions
% XArgsSelect is a list of (user defined) functions
% YName is a functor name
% Ys is a list of atoms of the form YName(A1,...,An) s.t.
% there exists an atom X in Xs satisfying the following conditions:
%   - for all F in XSelectFuns. F(Name,Pars) holds
%   - for all i in 1,...,n. Ai is the argument of X selected using
%                           the user defined function XArgsSelect[i]
filter([],_XSchema,_XSelectFuns,_XArgsSelect,_YName, []).
filter([X|Xs],XSchema,XSelectFuns,XArgsSelect,YName, [Y|Ys]) :-
  satisfy(X,XSchema,XSelectFuns),
  !,
  eval_proj_func(XArgsSelect,XSchema,X, YArgs),
  Y =.. [YName|YArgs],
  filter(Xs,XSchema,XSelectFuns,XArgsSelect,YName, Ys).
filter([_|Xs],XSchema,XSelectFuns,XArgsSelect,YName, Ys) :-
  filter(Xs,XSchema,XSelectFuns,XArgsSelect,YName, Ys).

% MODE: satisfy(+X,+XSchema,+XSelectFuns)
% SEMANTICS: satisfy(X,XSchema,XSelectFuns) holds
% if for all F(Name,Pars) in XSelectFuns.
%    XArg is the argument of X denoted by Name in XSchema and F(XArg,Pars) holds
satisfy(_X,_XSchema,[]).
satisfy(X,XSchema,[P|Ps]) :-
  P =.. [Pred,Name|Pars],
  arg(I,XSchema,Name),
  arg(I,X,XVal),
  F =.. [Pred,XVal|Pars],
  call(F),
  satisfy(X,XSchema,Ps).

% MODE: eval_proj_func(+XArgsSelect,+XSchema,+X, -XVals)
% SEMANTICS: XVals is the result of applying XArgsSelect to X.
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

%%% user defined predicates to be used as 3rd argument of filter/6
% MODE: isSubAtom(+A,+S)
% SEMANTICS: S is a subatom of A.
isSubAtom(A,S) :-
  % (https://www.swi-prolog.org/pldoc/doc_for?object=sub_atom/5)
  sub_atom(A,_,_,_,S).

% MODE: isNthSubAtom(+L,+I,+S)
% SEMANTICS: L at position I is an atom A and S is a subatom of A.
isNthSubAtom(L,I,S) :-
  nth1(I,L,A),
  subStr(A,S).

% MODE: isHttpMethod(+A)
% SEMANTICS: A is an atom and MockMvcRequestBuilders:M,
% with M in {get,post,put,delete}, is a sub-atom of A.
isHttpMethod(A) :-
  member(H,['get','post','put','delete']),
  atom_concat('MockMvcRequestBuilders:',H,M),
  sub_atom(A,_,_,_,M).

%%% user defined predicates to be used as 4th argument of filter/6
% MODE: head(+L,-H)
% SEMANTICS:
% if   L is a nonempty list,
% then H is the head of L
head(L,H) :-
  nonvar(L),
  L=[H|_],
  !.
% else H is bound to the atom domain_error/1
head(L,domain_error(empty_list)) :-
  nonvar(L),
  L=[],
  !.
head(_,domain_error(not_a_list)).

% MODE: httpMethod(+A,-H)
% SEMANTICS:
% if   A is bound to an atom and MockMvcRequestBuilders:M,
%      with M in {get,post,put,delete}, is a sub-atom of A,
% then H is bound to M
httpMethod(A,H) :-
  nonvar(A),
  member(M,['get','post','put','delete']),
  atom_concat('MockMvcRequestBuilders:',M,S),
  isSubAtom(A,S),
  !,
  H=M.
% else H is bound to the atom domain_error/1
httpMethod(_,domain_error(not_a_http_method)).

% MODE: method(+A,-M)
% SEMANTICS:
% if   A is bound to an atom of the form _:S:_,
% then M is bound to S
method(A,M) :-
  nonvar(A),
  atom_chars(A,C),
  append(_,[:|L],C), append(S,[:|_],L),
  atom_chars(M,S),
  !.
% else M is bound to the atom domain_error/1
method(_,domain_error(not_a_method)).

%% SIMILARITY RELATION ---------------------------------------------------------
% MODE: similar_endpoints(+EndpointsSrc,+Criterion,-TP1,-TP2,-Es1,-Es2)
% SEMANTICS: Es1 and Es2 are lists of endpoints of test programs TP1 and TP2,
% respectively, generated from lists of invokes representing either a trace
% (EndpointsSrc = trace) or a maximal invoke sequence (EndpointsSrc = miseq)
% that satisfy the similarity criterion Criterion.
similar_endpoints(EndpointsSrc,Criterion,TP1,TP2,Es1,Es2) :-
  ( EndpointsSrc == trace ; EndpointsSrc == miseq ),
  atom_concat(EndpointsSrc,'_endpoints',EndpointsFact),
  E1 =.. [EndpointsFact,(TP1,Es1)], E1, Es1\==[],
  E2 =.. [EndpointsFact,(TP2,Es2)], E2, TP1\==TP2,
  similar_endpoints(Criterion,Es1,Es2).
% similar_endpoints(eqset,Es1,Es2) holds if
similar_endpoints(eqset,Es1,Es2) :-
  % for all E1 in Es1, there exists a E2 in Es2 s.t E1 is similar to E2
  matching_endpoints(Es1,Es2),
  % for all E2 in Es2, there exists a E1 in Es1 s.t E1 is similar to E2
  matching_endpoints(Es2,Es1).
% similar_endpoints(subset,Es1,Es2) holds if
similar_endpoints(subset,Es1,Es2) :-
  % for all E1 in Es1, there exists a E2 in Es2 s.t E1 is similar to E2
  matching_endpoints(Es1,Es2).
% similar_endpoints(nonempty_intersection,Es1,Es2) holds if
% there exist E1 in Es1 and E2 in Es2 s.t. E1 is similar to E2
similar_endpoints(nonempty_intersection,Es1,Es2) :-
  member(E1,Es1),
  matching_endpoint(E1,Es2),
  !.
similar_endpoints(nonempty_intersection,Es1,Es2) :-
  member(E2,Es2),
  matching_endpoint(E2,Es1),
  !.

% MODE: endpoints(+InvokesLst,-EndpointLst)
% SEMANTICS: EndpointLst is the list of endpoint/4 atoms generated from the
% list of invokes InvokesLst
endpoints(InvokesLst,EndpointLst) :-
  filter(
    InvokesLst,
    invokes(testName,
            branchPoint,
            branchSequenceNumber,
            caller,
            callerProgramCounter,
            frameEpoch,
            pathCondition,
            callee,
            parameters),
    [isHttpMethod(callee)],
    [testName,method(caller),httpMethod(callee),head(parameters)],
    endpoint,
    EndpointLst
  ).

% MODE: matching_endpoints(+S1,+S2)
% SEMANTICS: S1 and S2 are lists of endpoints and for all E in S1
% there exists an element in S2 that satisfies matching_endpoint.
matching_endpoints([],_S2).
matching_endpoints([E|S1],S2) :-
  matching_endpoint(E,S2),
  matching_endpoints(S1,S2).

% MODE: matching_endpoint(+E1,+S)
% SEMANTICS: there exists in S an element E2 s.t.
% (*1*) E1 and E2 perform the same HTTP request, and
% (*2*) E1 and E2 match a regular expression REX representing a REST API
%       (the set of regular expressions is defined by rest_api_regex/1 facts)
matching_endpoint(E1,S) :-
  E1 = endpoint(_TP1,_Caller1,HTTPMethod,URI1), % E1 invokes HTTPMethod (*1*)
  atom_string(URI1,URI1Str),
  rest_api_regex(REX),    % REX is a REST API regular expression
  re_match(REX,URI1Str),  % the URI string URI1Str of E1 matches REX    (*2*)
  E2 = endpoint(_TP2,_Caller2,HTTPMethod,URI2),
  member(E2,S),           % E2 invokes the same HTTPMethod of E1        (*1*)
  atom_string(URI2,URI2Str),
  re_match(REX,URI2Str).  % the URI string URI2Str of E2 matches REX    (*2*)

% SEMANTICS: generate_and_assert_endpoints/1 generates the endpoints either
% from all the traces of all test programs, or
generate_and_assert_endpoints(trace) :-
  testPrograms(TPs),
  findall(
    (TP,ESeq),
    ( member(TP,TPs), trace(TP,Trace), endpoints(Trace,ESeq) ),
    Lst
  ),
  assert_lst(trace_endpoints,Lst).
% from all the maximal invoke sequences of all test programs entry points
generate_and_assert_endpoints(miseq) :-
  testPrograms(TPs),
  findall(
    (TP,ESeq),
    ( member(TP,TPs), testProgram_entry_point_caller(TP,Caller),
      maximalInvokeSequence(TP,Caller,ISeq), endpoints(ISeq,ESeq) ),
    Lst
  ),
  assert_lst(miseq_endpoints,Lst).

% assert a list of terms as facts of the form Name/1
assert_lst(_,[]).
assert_lst(Name,[T|Ts]) :-
  functor(Fact,Name,1),
  arg(1,Fact,T),
  assert(Fact),
  !,
  assert_lst(Name,Ts).
