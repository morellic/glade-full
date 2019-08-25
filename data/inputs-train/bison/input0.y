/*ab*/
%{
ab
%}
%union{ab}
%start input
%token <int_val> INTEGER_LITERAL
%type <int_val> exp
%left PLUS
%%
input: | exp { ab };
exp: INTEGER_LITERAL { ab } | exp PLUS exp { ab };
%%
ab
