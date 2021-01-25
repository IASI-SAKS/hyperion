%% Checking similarity of test programs
check(Src) :-
  retractall(invokes(_,_,_,_,_,_,_,_,_)),
  consult(Src),
  %%
  write('* Test Programs:'), nl,
  testPrograms(TPs),
  print_numbered_list(TPs), nl,
  %%
  write('* Checking sub_set_of_invoked_methods'), nl,
  check_sub_set_of_invoked_methods(TPs), nl, nl,
  %%
  write('* Checking eq_set_of_invoked_methods'), nl,
  check_eq_set_of_invoked_methods(TPs), nl, nl,
  %%
  write('* Checking eq_set_maximalInvokeSequence'), nl,
  check_eq_set_maximalInvokeSequence(TPs).

check_sub_set_of_invoked_methods(TPs) :-
  select(TP1,TPs,RTPs), member(TP2,RTPs),
  apply_check(sub_set_of_invoked_methods(TP1,TP2)).
check_sub_set_of_invoked_methods(_TPs).
%
check_eq_set_of_invoked_methods(TPs) :-
  select(TP1,TPs,RTPs), member(TP2,RTPs),
  apply_check(eq_set_of_invoked_methods(TP1,TP2)).
check_eq_set_of_invoked_methods(_TPs).
%
check_eq_set_maximalInvokeSequence(TPs) :-
  select(TP1,TPs,RTPs), tp_callers(TP1,TP1Callers), member(Caller,TP1Callers),
  member(TP2,RTPs),     tp_callers(TP2,TP2Callers), member(Caller,TP2Callers),
  apply_check(eq_set_maximalInvokeSequence(TP1,TP2,Caller)).
check_eq_set_maximalInvokeSequence(_TPs).
%
apply_check(P) :-
  write('Checking '), write(P), write(': '), (P-> write(true);write(false)), nl, fail.

% MODE: print_list(+Ls)
% SEMANTICS: print the elements of Ls on separate lines
print_list([]).
print_list([L|Ls]) :-
  write(L), nl,
  print_list(Ls).

% MODE: print_numbered_list(+Ls)
% SEMANTICS: print the elements of Ls on separate numbered lines
print_numbered_list(Ls) :-
  print_numbered_list(0,Ls).
print_numbered_list(N,[L|Ls]) :-
  M is N+1,
  writef("%5r. %w", [M,L]), nl,
  !,
  print_numbered_list(M,Ls).
print_numbered_list(_,[]).
