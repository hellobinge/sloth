grammar SlothPolicyRule;

policySet : globalPolicySet? localPolicySet?;

globalPolicySet : 'GLOBAL_POLICY' '{' policyStatement* '}';

localPolicySet : 'LOCAL_POLICY' '{' localPolicyStatement* '}';

localPolicyStatement : Identifier ',' Identifier '{' policyStatement* '}';

policyStatement : Identifier '{' statement '}';

statement
    :   block
    |   'ACCEPT'
    |   'REJECT'
    |   'if' parExpression statement ('else' statement)?
    ;

block : '{' statement? '}';

parExpression : '(' expression ')';

expression
    :   primary
    |   expression ('<=' | '>=' | '>' | '<') expression
    |   expression ('==' | '!=') expression
    |   expression '&&' expression
    |   expression '||' expression
    |   expression 'REGULAR' expression
    ;

primary
    :   '(' expression ')'
    |   jsonpath
    |   slothPredefined
    |   literal
    ;

jsonpath : '$.' dotExpression ('.' dotExpression)*;

dotExpression : identifierWithQualifier | Identifier;

identifierWithQualifier
    : Identifier '[]'
    | Identifier '[' IntegerLiteral ']'
    | Identifier '[?(' queryExpression ')]'
    ;

queryExpression
    :   queryExpression ('&&' queryExpression)+
    |   queryExpression ('||' queryExpression)+
    |   '*'
    |   '@.' Identifier
    |   '@.' Identifier '>' IntegerLiteral
    |   '@.' Identifier '<' IntegerLiteral
    |   '@.length-' IntegerLiteral
    |   '@.' Identifier '==' IntegerLiteral
    |   '@.' Identifier '==\'' IntegerLiteral '\''
    ;

slothPredefined
    :   'sloth.subject.' ('role' | 'user_id')
    |   'sloth.action.' ('method' | 'url' | 'query_string')
    |   'sloth.environment.' ('date' | 'time' | 'day_of_week')
    ;

literal
    :   IntegerLiteral
    |   FloatingPointLiteral
    |   CharacterLiteral
    |   StringLiteral
    |   BooleanLiteral
    |   NullLiteral
    ;

IntegerLiteral : NonzeroDigit Digit*;

FloatingPointLiteral : Digit* '.' Digit*;

CharacterLiteral :   '\'' SingleCharacter '\'';

StringLiteral : '"' SingleCharacter+ '"';

BooleanLiteral : 'true' | 'false';

NullLiteral : 'null';

fragment
NonzeroDigit : [1-9];

fragment
Digit : [0-9];

fragment
SingleCharacter : ~["\\];



GLOBAL_POLICY : 'GLOBAL_POLICY';
LOCAL_POLICY : 'LOCAL_POLICY';
ACCEPT : 'ACCEPT';
REJECT : 'REJECT';
LBRACE : '{';
RBRACE : '}';
IF : 'if';
ELSE : 'else';
EQUAL : '==';
NOTEQUAL : '!=';
LT : '<';
GT : '>';
LE : '<=';
GE : '>=';
AND : '&&';
OR : '||';
REGULAR : 'REGULAR';




Identifier :Letter LetterOrDigit*;

fragment
Letter
    :   [a-zA-Z$_]
    |   ~[\u0000-\u007F\uD800-\uDBFF]
    |   [\uD800-\uDBFF] [\uDC00-\uDFFF]
    ;

fragment
LetterOrDigit
    :   [a-zA-Z0-9$_]
    |   ~[\u0000-\u007F\uD800-\uDBFF]
    |   [\uD800-\uDBFF] [\uDC00-\uDFFF]
    ;


WS  : [ \t\r\n\u000C]+ -> skip;

COMMENT : '/*' .*? '*/' -> skip;

LINE_COMMENT : '//' ~[\r\n]* -> skip;
