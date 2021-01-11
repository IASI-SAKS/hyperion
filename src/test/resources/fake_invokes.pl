% reconsult('src/test/resources/fake_invokes.pl').

%  a1(...) {
%     b1(...);
%     c1(...);
%     while (...)
%        w1(...);
%     f1(...);
%     f1(...);
%  }
%
%  c1(...) {
%    if(...)
%      d1(...);
%      d2(...);
%    else
%      e1(...);
%  }

% t1
invokes('t1', [1],          1, 'a1', 11,  11, [], 'b1', []).
invokes('t1', [1],          3, 'a1', 12,  11, [], 'c1', []).
invokes('t1', [1,1],        5, 'c1', 18,  13, [], 'd1', []).
invokes('t1', [1,1],        7, 'c1', 19,  13, [], 'd2', []).
invokes('t1', [1,1,1,1],    9, 'a1', 14,  11, [], 'w1', []).
invokes('t1', [1,1,1,1],   11, 'a1', 14,  11, [], 'w1', []).
invokes('t1', [1,1,1,1],   13, 'a1', 14,  11, [], 'w1', []).
invokes('t1', [1,1,1,1],   15, 'a1', 15,  11, [], 'f1', []).
invokes('t1', [1,1,1,1],   17, 'a1', 16,  11, [], 'f1', []).
invokes('t1', [1,2],       19, 'c1', 21,  13, [], 'e1', []).
invokes('t1', [1,2,1,1,1], 21, 'a1', 14,  11, [], 'w1', []).
invokes('t1', [1,2,1,1,1], 23, 'a1', 14,  11, [], 'w1', []).
invokes('t1', [1,2,1,1,1], 25, 'a1', 15,  11, [], 'f1', []).
invokes('t1', [1,2,1,1,1], 27, 'a1', 16,  11, [], 'f1', []).

% t2
invokes('t2', [1],          1, 'a1', 11,  11, [], 'b1', []).
invokes('t2', [1],          3, 'a1', 12,  11, [], 'c1', []).
invokes('t2', [1,1],        5, 'c1', 18,  13, [], 'd1', []).
invokes('t2', [1,1],        7, 'c1', 19,  13, [], 'd2', []).
invokes('t2', [1,1,1],      9, 'a1', 14,  11, [], 'w1', []).
invokes('t2', [1,1,1],     15, 'a1', 15,  11, [], 'f1', []).
invokes('t2', [1,1,1],     17, 'a1', 16,  11, [], 'f1', []).
invokes('t2', [1,2],       18, 'c1', 21,  13, [], 'e1', []).
invokes('t2', [1,2,1],     19, 'a1', 15,  11, [], 'f1', []).
invokes('t2', [1,2,1],     23, 'a1', 16,  11, [], 'f1', []).

% ----------------------------------------------------------
%  m(...) {
%    if(...)
%      a(...)
%      b(...)
%      c(...)
%    else
%      a(...)
%      b(...)
%    if(...)
%      b(...)
%      c(...)
% }

% t3
invokes('t3', [1,1],    1, 'm', 11,  3, [], 'a', []).
invokes('t3', [1,1],    3, 'm', 12,  3, [], 'b', []).
invokes('t3', [1,1],    5, 'm', 13,  3, [], 'c', []).
invokes('t3', [1,2],    7, 'm', 14,  3, [], 'a', []).
invokes('t3', [1,2],    9, 'm', 15,  3, [], 'b', []).

% t4
invokes('t4', [1,1],    1, 'm', 11,  3, [], 'a', []).
invokes('t4', [1,1],    3, 'm', 12,  3, [], 'b', []).
invokes('t4', [1,1],    5, 'm', 13,  3, [], 'c', []).

% t5
invokes('t5', [1,1],    1, 'm', 11,  3, [], 'a', []).
invokes('t5', [1,1],    3, 'm', 12,  3, [], 'b', []).
invokes('t5', [1,1],    5, 'm', 13,  3, [], 'c', []).
invokes('t5', [1,1,1],  7, 'm', 16,  3, [], 'b', []).
invokes('t5', [1,1,1],  9, 'm', 17,  3, [], 'c', []).

% t6
invokes('t6', [1,1],    1, 'm', 11,  0, [], 'a', []).
invokes('t6', [1,1],    3, 'm', 12,  0, [], 'b', []).
invokes('t6', [1,1],    5, 'm', 13,  0, [], 'c', []).
invokes('t6', [1,1,1],  7, 'm', 16,  0, [], 'b', []).
invokes('t6', [1,1,1],  9, 'm', 17,  0, [], 'c', []).
invokes('t6', [1,2],   11, 'm', 14,  0, [], 'a', []).
invokes('t6', [1,2],   13, 'm', 15,  0, [], 'b', []).
invokes('t6', [1,2,1], 15, 'm', 16,  0, [], 'b', []).
invokes('t6', [1,2,1], 17, 'm', 17,  0, [], 'c', []).

% t7
invokes('t7', [1,1],    1, 'm', 14,  0, [], 'a', []).
invokes('t7', [1,1],    3, 'm', 15,  0, [], 'b', []).
invokes('t7', [1,1,1],  5, 'm', 16,  0, [], 'b', []).
invokes('t7', [1,1,1],  7, 'm', 17,  0, [], 'c', []).
invokes('t7', [1,2],    9, 'm', 11,  0, [], 'a', []).
invokes('t7', [1,2],   11, 'm', 12,  0, [], 'b', []).
invokes('t7', [1,2],   13, 'm', 13,  0, [], 'c', []).
invokes('t7', [1,2,1], 15, 'm', 16,  0, [], 'b', []).
invokes('t7', [1,2,1], 17, 'm', 17,  0, [], 'c', []).
