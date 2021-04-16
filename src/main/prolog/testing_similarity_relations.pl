:- consult(similarity_relations).

%% Checking similarity of test programs
check(Src) :-
  retractall(invokes(_,_,_,_,_,_,_,_,_)),
  consult(Src),
  %%
  tell('testing_similarity_relations.out.txt'),
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
  write('* Checking sub_set_of_maximalInvokeSequences'), nl,
  check_sub_set_of_maximalInvokeSequences(TPs), nl, nl,
  %%
  write('* Checking eq_set_of_maximalInvokeSequences'), nl,
  check_eq_set_of_maximalInvokeSequences(TPs),
  told.

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
check_sub_set_of_maximalInvokeSequences(TPs) :-
  select(TP1,TPs,RTPs), tp_callers(TP1,TP1Callers), member(Caller,TP1Callers),
  member(TP2,RTPs),     tp_callers(TP2,TP2Callers), member(Caller,TP2Callers),
  apply_check(sub_set_of_maximalInvokeSequences(TP1,TP2,Caller)).
check_sub_set_of_maximalInvokeSequences(_TPs).
%
check_eq_set_of_maximalInvokeSequences(TPs) :-
  select(TP1,TPs,RTPs), tp_callers(TP1,TP1Callers), member(Caller,TP1Callers),
  member(TP2,RTPs),     tp_callers(TP2,TP2Callers), member(Caller,TP2Callers),
  apply_check(eq_set_of_maximalInvokeSequences(TP1,TP2,Caller)).
check_eq_set_of_maximalInvokeSequences(_TPs).
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

% MODE: print_atom(+Ls)
% SEMANTICS: print the atoms in L on separate lines
print_atom_list([]).
print_atom_list([L|Ls]) :-
  print_atom(L), nl,
  !,
  print_atom_list(Ls).
%
print_atom(A) :-
  A =.. [P|As],
  write(P),
  ( As == [] ->
    write('.')
  ;
    ( write('('), nl, print_qatom_args(As), nl, write(').') )
  ).
%
print_atom_args([L]) :-
  write(' '), write(L).
print_atom_args([L|Ls]) :-
  write(' '), write(L), nl,
  write(' '), write(L), write(','), nl,
  !,
  print_atom_args(Ls).
%
print_qatom_args([L]) :-
  write(' '''), write(L), write('''').
print_qatom_args([L|Ls]) :-
  write(' '''), write(L), write(''','), nl,
  !,
  print_qatom_args(Ls).

% calls:
% generate_and_assert_endpoints, similarEndpoints, similarityScore, and
% prints the answers
print_similar_endpoints(EpSrc) :-
  write('Generating endpoints ... '), flush_output,
  generate_and_assert_endpoints(EpSrc),
  write('done.'),
  atom_concat('similarEndpoints-',EpSrc,FileNamePrefix),
  atom_concat(FileNamePrefix,'-report.txt',TXTFileName),
  open(TXTFileName,write,Fd1,[alias(txt)]),
  atom_concat(FileNamePrefix,'-report.csv',CSVFileName),
  open(CSVFileName,write,Fd2,[alias(csv)]),
  assert(ln(1)),
  %print_similar_endpoints_answers(EpSrc,nonemptyEqSet),
  %print_similar_endpoints_answers(EpSrc,nonemptySubSet),
  print_similar_endpoints_answers(EpSrc,overlappingSet),
  close(Fd1),
  close(Fd2).
%
print_similar_endpoints_answers(EpSrc,SimCr) :-
  print_similar_endpoints_answer(EpSrc,SimCr),
  fail.
print_similar_endpoints_answers(_EpSrc,_SimCr).
%
print_similar_endpoints_answer(EpSrc,SimCr) :-
  write(txt,'----------------------------------'),     nl(txt),
  write(txt,'Querying similarTestProgs/6'),            nl(txt), nl(txt),
  write(txt,'* Endpoints source: '), write(txt,EpSrc), nl(txt),
  write(txt,'* Criterion:        '), write(txt,SimCr), nl(txt),
  write(txt,'----------------------------------'),     nl(txt), nl(txt),
  similarTestProgs(EpSrc,SimCr,TP1,TP2,Es1,Es2),
  similarityScore(SimCr,Es1,Es2,Score),
  retract(ln(N)),
  write_txt(N,TP1,TP2,Es1,Es2,Score),
  write_csv(N,SimCr,TP1,TP2,Score),
  M is N+1,
  assert(ln(M)).
%
write_txt(N,TP1,TP2,Es1,Es2,Score) :-
  set_output(txt),
  write('* ID: '),             write(N),       nl,
  write('* Test Program 1: '), write(TP1),     nl, nl,
  print_atom_list(Es1),                        nl,
  write('* Test Program 2: '), write(TP2),     nl, nl,
  print_atom_list(Es2),                        nl, nl,
  !,
  write('* Score: '),          write(Score),   nl, nl,
  write('----------------------------------'), nl.
%
write_csv(N,SimCr,TP1,TP2,Score) :-
  set_output(csv),
  write(N), write(','),
  write(SimCr), write(','),
  write(TP1), write(','),
  write(TP2), write(','),
  write(Score), write(','), nl.
