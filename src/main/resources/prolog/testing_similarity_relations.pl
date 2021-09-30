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
  write('* Checking sub_set_of_invoke_sequences'), nl,
  check_sub_set_of_invoke_sequences(TPs), nl, nl,
  %%
  write('* Checking eq_set_of_invoke_sequences'), nl,
  check_eq_set_of_invoke_sequences(TPs),
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
check_sub_set_of_invoke_sequences(TPs) :-
  select(TP1,TPs,RTPs), tp_callers(TP1,TP1Callers), member(Caller,TP1Callers),
  member(TP2,RTPs),     tp_callers(TP2,TP2Callers), member(Caller,TP2Callers),
  apply_check(sub_set_of_invoke_sequences(TP1,TP2,Caller)).
check_sub_set_of_invoke_sequences(_TPs).
%
check_eq_set_of_invoke_sequences(TPs) :-
  select(TP1,TPs,RTPs), tp_callers(TP1,TP1Callers), member(Caller,TP1Callers),
  member(TP2,RTPs),     tp_callers(TP2,TP2Callers), member(Caller,TP2Callers),
  apply_check(eq_set_of_invoke_sequences(TP1,TP2,Caller)).
check_eq_set_of_invoke_sequences(_TPs).
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

% :- dynamic endpoints/3.
% :- multifile endpoints/3.
% similarity_from_endpoints_dir(Dir,_EpSrc,SimCr) :-
%   directory_files(Dir,Files),
%   delete(Files,'.',Files1),
%   delete(Files1,'..',InFiles),
%   retractall(endpoints(_,_,_)),
%   retractall(invokes(_,_,_,_,_,_,_,_,_)),
%   write('Consulting endpoints/3 from:'), nl,
%   member(InFileName,InFiles),
%   write(InFileName), nl, flush_output,
%   atom_concat(Dir,InFileName,InFile),
%   consult(InFile),
%   fail.
% similarity_from_endpoints_dir(_Dir,EpSrc,SimCr) :-
%   write('done.'), flush_output,
%   print_similarity(EpSrc,SimCr).

% SEMANTICS: generate and assert endpoints from the invokes in File,
% run similarity predicates and prints the answers to csv and txt files
similarity_from_invokes_file(File,EpSrc,SimCr) :-
  retractall(endpoints(_,_,_)),
  retractall(invokes(_,_,_,_,_,_,_,_,_)),
  write('Consulting invokes/9.'), nl,
  consult(File),
  retractall(counter(_)), assert(counter(1)),
  write('Generating endpoints ... '), flush_output,
  generate_and_assert_endpoints(EpSrc),
  write('done.'), flush_output,
  print_similarity(EpSrc,SimCr).

% SEMANTICS: generate and assert endpoints from the invokes in the files of Dir,
% run similarity predicates and prints the answers to csv and txt files
similarity_from_invokes_dir(Dir,EpSrc,_SimCr) :-
  retractall(endpoints(_,_,_)),
  directory_files(Dir,Files),
  delete(Files,'.',Files1),
  delete(Files1,'..',InFiles),
  member(InFileName,InFiles),
  atom_concat(Dir,InFileName,InFile),
  retractall(invokes(_,_,_,_,_,_,_,_,_)),
  write('Consulting invokes/9 from:'), write(InFileName), nl,
  consult(InFile),
  retractall(counter(_)), assert(counter(1)),
  write('Generating endpoints ... '), flush_output,
  generate_and_assert_endpoints(EpSrc),
  write('done.'), flush_output,
  fail.
similarity_from_invokes_dir(_Dir,EpSrc,SimCr) :-
  print_similarity(EpSrc,SimCr).

% SEMANTICS: generates the endpoints either
% from all the traces of all test programs, or
generate_and_assert_endpoints(trace) :-
  testPrograms(TPs),
  member(TP,TPs),
  trace(TP,Trace),
  retract(counter(N)), M is N+1, assert(counter(M)),
  write(N), write(' '), flush_output,
  endpoints(Trace,ESeq),
  assert(endpoints(trace,TP,ESeq)),
  fail.
% from all the invoke sequences of all entry points
generate_and_assert_endpoints(iseq) :-
  testPrograms(TPs),
  member(TP,TPs),
  testProgram_entry_point_caller(TP,Caller),
  invoke_sequence(TP,Caller,ISeq),
  retract(counter(N)), M is N+1, assert(counter(M)),
  write(N), write(' '), flush_output,
  endpoints(ISeq,ESeq),
  assert(endpoints(iseq,TP,ESeq)),
  fail.
generate_and_assert_endpoints(_) :-
  retractall(counter(_)).

% SEMANTICS: open the txt and csv files and run
% printall_similarity_answers
print_similarity(EpSrc,SimCr) :-
  atom_concat('similarEndpoints-',EpSrc,FileNamePrefix),
  atom_concat(FileNamePrefix,'-report.txt',TXTFileName),
  open(TXTFileName,write,Fd1,[alias(txt)]),
  atom_concat(FileNamePrefix,'-report.csv',CSVFileName),
  open(CSVFileName,write,Fd2,[alias(csv)]),
  assert(ln(1)),
  printall_similarity_answers(EpSrc,SimCr),
  close(Fd1),
  close(Fd2).

% SEMANTICS: gets all answers from similarity predicates and
% write them to csv and txt files
printall_similarity_answers(EpSrc,SimCr) :-
  print_similarity_ans(EpSrc,SimCr),
  fail.
printall_similarity_answers(_EpSrc,_SimCr).

% SEMANTICS: run similar_tp/6 and similarity_score/4
% write the answer to txt and csv files
print_similarity_ans(EpSrc,SimCr) :-
  write(txt,'----------------------------------'),     nl(txt),
  write(txt,'Querying similar_tp/6'),                  nl(txt), nl(txt),
  write(txt,'* Endpoints source: '), write(txt,EpSrc), nl(txt),
  write(txt,'* Criterion:        '), write(txt,SimCr), nl(txt),
  write(txt,'----------------------------------'),     nl(txt), nl(txt),
  similar_tp(EpSrc,SimCr,TP1,TP2,Es1,Es2),
  similarity_score(SimCr,Es1,Es2,Score),
  retract(ln(N)),
  write_txt(N,TP1,TP2,Es1,Es2,Score),
  write_csv(N,SimCr,TP1,TP2,Score),
  M is N+1,
  assert(ln(M)).

% SEMANTICS: write answer to txt
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

% SEMANTICS: write answer to csv
write_csv(N,SimCr,TP1,TP2,Score) :-
  set_output(csv),
  write(N),     write(','),
  write(SimCr), write(','),
  write(TP1),   write(','),
  write(TP2),   write(','),
  write(Score), nl.

% SEMANTICS: for each test program TP,
% generate a file invokes__TP.pl including the invokes facts of TP.
partition_invokes(InvokesFile) :-
  atom_concat(InvokesFile,'-partition',PartitionDir),
  make_directory(PartitionDir),
  retractall(invokes(_,_,_,_,_,_,_,_,_)),
  consult(InvokesFile),
  testPrograms(TPs),
  member(TP,TPs),
  atom_concat(PartitionDir,'/invokes__',Prefix1),
  atom_concat(Prefix1,TP,Prefix2),
  atom_concat(Prefix2,'.pl',FileName),
  tell(FileName),
  partition_invokes_testProgram(TP),
  told,
  fail.
partition_invokes(_InvokesFile).

% SEMANTICS: generate a file invokes__TP.pl including the invokes facts of TP.
partition_invokes_testProgram(TP) :-
  invokes_component(Invokes,testProgram,TP),
  Invokes,
  write_term(Invokes,[quoted(true)]), write('.'), nl,
  fail.
partition_invokes_testProgram(_TP).

% SEMANTICS: for each file InFile in Dir,
% create a file endpoints__InFile including the endpoits facts generated from
% the EpSrc source of invokes facts in InFile.
generate_and_write_endpoints_dir(Dir,EpSrc) :-
  directory_files(Dir,Files),
  delete(Files,'.',Files1),
  delete(Files1,'..',InFiles),
  member(InFileName,InFiles),
  atom_concat(Dir,InFileName,InFile),
  retractall(endpoints(_,_,_)),
  retractall(invokes(_,_,_,_,_,_,_,_,_)),
  consult(InFile),
  write('Generating endpoints from: '), writeln(InFileName), flush_output,
  retractall(counter(_)), assert(counter(1)),
  generate_and_assert_endpoints(EpSrc),
  writeln('done.'), flush_output,
  atom_concat('endpoints__',InFileName,OutFileName),
  atom_concat(Dir,OutFileName,OutFile),
  tell(OutFile),
  write_endpoints,
  told,
  fail.
generate_and_write_endpoints_dir(_Dir,_EpSrc).

% SEMANTICS: write endpoints facts.
write_endpoints :-
  functor(Endpoints,endpoints,3),
  Endpoints,
  write_term(Endpoints,[quoted(true)]), write('.'), nl,
  fail.
write_endpoints.
