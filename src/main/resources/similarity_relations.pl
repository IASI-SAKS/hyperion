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

% ------------------------------------------------------------------------------
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
  % invoke(TP,_,_,_,_,M,_) succeeds
  findall(M, invokes(TP,_,_,_,_,_,_,M,_), L).

% ------------------------------------------------------------------------------
% SEMANTICS: invokeSequence(TP,Caller,Seq)
% Seq is a sequence of direct invocations performed by Caller in TP
invokeSequence(TP,Caller,ISeq) :-
  Invokes = invokes(TP,_,_,Caller,_,_,_,_,_), Invokes,
  iseq(Invokes,ISeq).

% SEMANTICS: invokeMaximalSequence(TP,Caller,Seq)
% Seq is a maximal sequence of direct invocations performed by Caller in TP
invokeMaximalSequence(TP,Caller,ISeq) :-
  method_first_invokes(TP,Caller,FirstInvokes),
  method_last_invokes(TP,Caller,LastInvokes),
  FirstInvokes \= LastInvokes, % sequences of at least two method invocations
  iseq(FirstInvokes,ISeq),
  append([FirstInvokes|_],[LastInvokes],ISeq).

% SEMANTICS: iseq(I,ISeq)
% ISeq is the sequence of invocations performed by Caller in TP starting from I
iseq(I1,[I1|Is]) :-
  I1 = invokes(TP,_,_,Caller,_,_,_,_,_),
  I2 = invokes(TP,_,_,Caller,_,_,_,_,_),
  next(I1,I2),
  iseq(I2,Is).
iseq(I1,[I1]).

% SEMANTICS: next(Invokes1,Invokes2)
% Invokes2 is the subsequent invocation performed by Caller in TP
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
% either it has the same branching point BP of Invokes1 and Invokes2 and
% its seq.no. is greater than that of Invokes1 and less than that of Invokes2
exists_intermediate_invokes(Invokes1,Invokes2) :-
  Invokes1 = invokes(TP,BP1,SN1,Caller,_,_,_,_,_),
  Invokes2 = invokes(TP,BP2,SN2,Caller,_,_,_,_,_),
  BP1 = BP2,
  InvokesM = invokes(TP,BP2, SN,Caller,_,_,_,_,_), InvokesM,
  SN1 < SN, SN < SN2,
  !.
% or its branching point BP extends the branching point of Invokes1, and
% it is a prefix of the branching point BP2 of Invokes2 such that either
% (*) BP2 extends BP or (**) BP = BP2 with a seq.no. less than that of Invokes2
exists_intermediate_invokes(Invokes1,Invokes2) :-
  Invokes1 = invokes(TP,BP1,  _,Caller,_,_,_,_,_),
  Invokes2 = invokes(TP,BP2,SN2,Caller,_,_,_,_,_),
  BP1 \= BP2,
  InvokesM = invokes(TP,BP,  SN,Caller,_,_,_,_,_), InvokesM,
  append(BP1,[_|_],BP), append(BP,Infix,BP2),
  ( Infix = [_|_] % (*)
  ;
    ( Infix = [], SN < SN2 ) % (**)
  ).

% Invokes is the first invocation performed by Caller in TP
method_first_invokes(TP,Caller,Invokes) :-
  Invokes = invokes(TP,_,_,Caller,_,_,_,_,_), Invokes,
  \+ exist_preceeding_invokes(Invokes).
% method_first_invokes utility predicate
exist_preceeding_invokes(Invokes) :-
  next(_,Invokes).
% Invokes is the last invocation performed by Caller in TP
method_last_invokes(TP,Caller,Invokes) :-
  Invokes = invokes(TP,_,_,Caller,_,_,_,_,_), Invokes,
  \+ exist_succeeding_invokes(Invokes).
% method_last_invokes utility predicate
exist_succeeding_invokes(Invokes) :-
  next(Invokes,_).

% SEMANTICS: callees(Is,Ms)
% Ms is the list of calee occurring in the list of invokes Is
callees([],[]).
callees([I|Is],[M|Ms]) :-
  I = invokes(_,_,_,_,_,_,_,M,_),
  callees(Is,Ms).
