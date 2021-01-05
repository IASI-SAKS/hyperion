:- set_prolog_flag(character_escapes,false).

%% Checking similarity of test programs
check_sim(Src) :-
  retractall(invokes(_,_,_,_,_,_,_,_,_)),
  consult(Src),
  % S is the set of Test Programs
  setof(TP, [A,B,C,D,E,F,G,H]^invokes(TP,A,B,C,D,E,F,G,H), S),
  member(Sim,[sub_set_of_invoked_methods,
              eq_set_of_invoked_methods]),
  % SelTP is the Test Program selected program from S and R is S \ {SelTP}
  select(SelTP,S,R),
  nl, write('Checking '), write(Sim), write(' between  '), write(SelTP), write('  and '), nl,
  % checks Sim between SelTP and all test programs in R
  check_sim_all(SelTP,Sim,R),
  fail.
check_sim(_Src) :-
  nl, write('done!').

check_sim_all(_,_,[]).
check_sim_all(SelTP,Sim,[TP|TPs]) :-
  write(' '), write(TP), write(' '),
  ( call(Sim,SelTP,TP) -> write('PASS'); write('FAIL') ), nl,
  check_sim_all(SelTP,Sim,TPs).

% MODE: invoked_methods(+TP,?L)
% SEMANTICS: L is the list of methods invoked by the test program TP
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
invoked_methods(TP,L) :-
  % L is the list of all the instances of M for which
  % invoke(TP,_,_,_,_,_,_,M,_) succeeds
  findall(M, invokes(TP,_,_,_,_,_,_,M,_), L).

% SIMILARITY relations ---------------------------------------------------------
% MODE: sub_set_of_invoked_methods(+TP1,+TP2)
% SEMANTICS: sub_set_of_invoked_methods(+TP1,+TP2) holds if all methods invoked
% by TP1 are invoked by TP2.
sub_set_of_invoked_methods(TP1,TP2) :-
  exists_method_not_invoked(TP1,TP2),
  !,
  fail.
sub_set_of_invoked_methods(_TP1,_TP2).

% MODE: eq_set_of_invoked_methods(+TP1,+TP2)
% SEMANTICS: eq_set_of_invoked_methods(+TP1,+TP2) holds if TP1 and TP2 invoke
% the same set of methods.
eq_set_of_invoked_methods(TP1,TP2) :-
  sub_set_of_invoked_methods(TP1,TP2),
  sub_set_of_invoked_methods(TP2,TP1).

% MODE: exists_method_not_invoked(+TP1,+TP2)
% SEMANTICS: there exists a method M invoked by TP1 that is not invoked by TP2
exists_method_not_invoked(TP1,TP2) :-
  tp_invokes_m(TP1,M),
  \+ tp_invokes_m(TP2,M).

% MODE: tp_invokes_m(?TP,?M)
% SEMANTICS: TP invokes M
tp_invokes_m(TP,M) :-
  invokes(TP,_,_,_,_,_,_,M,_).

% MODE: eq_set_maximalInvokeSequence(+TP1,+TP2,+Caller)
% SEMANTICS: eq_set_maximalInvokeSequence(TP1,TP2,Caller) holds if the set of
% maximal sequences of direct invocations performed by Caller in TP1 is the same
% as the set of maximal sequences of direct invocations performed by Caller in TP2
% (library(ordsets): https://www.swi-prolog.org/pldoc/man?section=ordsets)
eq_set_maximalInvokeSequence(TP1,TP2,Caller) :-
  findall(MSeq, (maximalInvokeSequence(TP1,Caller,ISeq), callees(ISeq,MSeq)), MSeq1Lst),
  findall(MSeq, (maximalInvokeSequence(TP2,Caller,ISeq), callees(ISeq,MSeq)), MSeq2Lst),
  list_to_ord_set(MSeq1Lst,S1), % library(ordsets) predicate
  list_to_ord_set(MSeq2Lst,S2),
  ord_seteq(S1,S2).             % library(ordsets) predicate

% ------------------------------------------------------------------------------
% SEMANTICS: invokeSequence(TP,Caller,ISeq)
% ISeq is a sequence of direct invocations performed by Caller in TP
invokeSequence(TP,Caller,ISeq) :-
  Invokes = invokes(TP,_,_,Caller,_,_,_,_,_), Invokes,
  iseq(Invokes,ISeq).

% SEMANTICS: maximalInvokeSequence(TP,Caller,ISeq)
% ISeq is a maximal sequence of direct invocations performed by Caller in TP
maximalInvokeSequence(TP,Caller,ISeq) :-
  method_first_invokes(TP,Caller,FirstInvokes),
  method_last_invokes(TP,Caller,LastInvokes),
  FirstInvokes \= LastInvokes, % sequences with at least two method invocations
  iseq(FirstInvokes,ISeq),
  append([FirstInvokes|_],[LastInvokes],ISeq).

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

% Invokes is the first invocation performed by Caller in TP
method_first_invokes(TP,Caller,Invokes) :-
  Invokes = invokes(TP,_,_,Caller,_,_,_,_,_), Invokes,
  \+ exists_preceeding_invokes(Invokes).
% method_first_invokes utility predicate
exists_preceeding_invokes(Invokes) :- next(_,Invokes).
% Invokes is the last invocation performed by Caller in TP
method_last_invokes(TP,Caller,Invokes) :-
  Invokes = invokes(TP,_,_,Caller,_,_,_,_,_), Invokes,
  \+ exists_succeeding_invokes(Invokes).
% method_last_invokes utility predicate
exists_succeeding_invokes(Invokes) :- next(Invokes,_).

% SEMANTICS: callees(Is,Ms)
% Ms is the list of calee occurring in the list of invokes Is
callees([],[]).
callees([I|Is],[M|Ms]) :-
  I = invokes(_,_,_,_,_,_,_,M,_),
  callees(Is,Ms).
