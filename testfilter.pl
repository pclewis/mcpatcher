#!/usr/bin/perl -p

use strict;

s/\b[a-z]+\.class/xx.class/g;
s/L[a-z]+;/Lxx;/g;
s/ [a-z]+/ xx/g if /multiple matches: /;
s/\b[a-z]+(\(\S*\))/xx$1/g;

s/(matches|->) [a-zA-Z_]{1,3}\b/$1 xx/g if /field|string replace/;
s/(matches|->) [a-zA-Z_]{1,3} (\w*\(\S*\)\S+)/$1 xx $2/g if /method|class ref/;
s/-> [a-z\/M]+\.[a-zA-Z<>]+ /-> xx.x /g if /(class|method|field) ref/;

s/@[[:digit:]]+/@.../g;
s/-> instruction \d+/-> instruction .../;

s/((INVOKE(VIRTUAL|STATIC|INTERFACE|SPECIAL))|((GET|PUT)(FIELD|STATIC))|NEW|CHECKCAST|INSTANCEOF|IF_?[A-Z]+)( 0x[[:xdigit:]]{2}){2}/$1 0x.. 0x../g;
s/LDC 0x[[:xdigit:]]{2}/LDC 0x../g;
s/(LDC2?_W) 0x[[:xdigit:]]{2} 0x[[:xdigit:]]{2}/$1 0x.. 0x../g;

s/^(OS|JVM|Classpath): .*/$1: .../;
