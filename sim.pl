% NOTES: think about using
% Ordered set manipulation:
% https://www.swi-prolog.org/pldoc/man?section=ordsets

% MODE: same_SET_of_invoked_methods(+TP1,+TP2)
% SEMANTICS: the test programs TP1 and TP2 invoke the same *set* of methods
% same_SET_of_invoked_methods(TP1,TP2) :-
%   invoked_methods(TP1,S1),
%   invoked_methods(TP2,S2),
%   eq_set(S1,S2).

% MODE: invoked_methods(+TP,?L)
% SEMANTICS: L is the list of methods invoked by the test program TP
invoked_methods(TP,L) :-
  % L is the list of all the instances of IM for which
  % invoke(TP,_,_,_,_,IM,_) succeeds
  findall(IM, invokes(TP,_,_,_,_,IM,_), L).

% invokes/7 argument meaning
% invokes(
% 1  "nome" del test,
% 2  branch point,
% 3  sequence number,
% 4  program point,
% 5  path condition,
% 6  metodo chiamato,
% 7  parametri (WIP))

% MODE: eq_set(+S1,+S2)
% TYPE: eq_set(ground(list(term)),ground(list(term)))
% SEMANTICS: eq_set(S1,S2) holds if S1 and S2 represent two equivalent sets.
eq_set(S1,S2) :-
  sub_set(S1,S2),
  sub_set(S2,S1).

% MODE: sub_set(+S1,+S2)
% TYPE: sub_set(ground(list(term)),ground(list(term)))
% SEMANTICS: sub_set(S1,S2) holds if all elements of S1 belong to S2.
sub_set([],_).
sub_set([S|S1],S2) :-
  member(S,S2),
  !,
  sub_set(S1,S2).

% "pretty" print utility predicate
print_list([]).
print_list([L|Ls]) :-
  write(L), nl,
  print_list(Ls).

% % alternative implementation - v1
% same_SET_of_invoked_methods(TP1,TP2) :-
%   \+ exists_method_not_invoked(TP1,TP2),
%   \+ exists_method_not_invoked(TP2,TP1).
% % alternative implementation - v2
% same_SET_of_invoked_methods(TP1,TP2) :-
%   exists_method_not_invoked(TP1,TP2),
%   !,
%   fail.
% same_SET_of_invoked_methods(TP1,TP2) :-
%   exists_method_not_invoked(TP2,TP1),
%   !,
%   fail.
% same_SET_of_invoked_methods(_TP1,_TP2).

% MODE: exists_method_not_invoked(+TP1,+TP2)
% SEMANTICS: there exists a method M invoked by TP1 that is not invoked by TP2
exists_method_not_invoked(TP1,TP2) :-
  tp_invokes_m(TP1,M),
  \+ tp_invokes_m(TP2,M).

% MODE: tp_invokes_m(?TP,?M)
% SEMANTICS: TP invokes M
tp_invokes_m(TP,M) :-
  invokes(TP,_,_,_,_,M,_).
