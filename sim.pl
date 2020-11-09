% same_SET_of_invoked_methods(+TP1,+TP2)
% SEMANTICS: TP1 and TP2 invoke the same *set* of methods
same_SET_of_invoked_methods(TP1,TP2) :-
  set_of_invoked_methods(TP1,S1),
  set_of_invoked_methods(TP2,S2),
  S1 == S2.

% same_SET_of_invoked_methods(+TP1,+TP2)
% SEMANTICS: TP1 and TP2 invoke the same *sequence* of methods
same_SEQ_of_invoked_methods(TP1,TP2) :-
  seq_of_invoked_methods(TP1,S1),
  seq_of_invoked_methods(TP2,S2),
  S1 == S2.

% invoke template
% invokes(
%   "nome" del test,
%   branch point,
%   sequence number,
%   program point,
%   path condition,
%   metodo chiamato,
%   parametri (WIP))
% invoke instance:
% invokes(
%   com.fullteaching.backend.e2e.rest.UserRestTest:testCreateUserOk,
%   [1],
%   12,
%   com/fullteaching/backend/e2e/rest/UserRestTest:()V@4,
%   [({R0}, Object[4460] (fresh))], [({R0}, {ROOT}:this)],
%   com/fullteaching/backend/e2e/rest/UserRestTest:createUser:(Ljava/lang/String;)I,
%   [null, ])

set_of_invoked_methods(TP,Set) :-
  % S is the list of all the instances of IM for which invoke(TP,_,_,_,_,IM,_) succeeds
  findall(IM, invoke(TP,_,_,_,_,IM,_), S),
  sort(S,Set). % sort removes duplicates
  % alternative implementation:
  % setof(IM, [BP,SN,PP,PC,PL]^invoke(TP,BP,SN,PP,PC,IM,PL), Set).


seq_of_invoked_methods(TP,Seq) :-
  findall(SN-IM, invoke(TP,_,SN,_,_,IM,_), S),
  keysort(S,SeqP),  % sort the list of pairs SN-IM by SN
  rem1st(SeqP,Seq). % remove the 1st component from the elements in SeqP
  % alternative implementation:
  % findall(IM, member(_-IM,SeqP), Seq).
%
rem1st([],[]).
rem1st([_-IM|L1],[IM|L2]) :-
  rem1st(L1,L2).
