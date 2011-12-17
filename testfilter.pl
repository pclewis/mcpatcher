#!/usr/bin/perl -p

use strict;

s/-> [a-z]+ @[[:digit:]]+/-> xx @.../g if /(method|field) ref/ || /string replace/;
s/-> [a-z\/M]+\.[a-zA-Z<>]+ /-> xx.x /g if /(method|field) ref/;
s/@[[:digit:]]+/@.../g;
s/((INVOKE(VIRTUAL|STATIC|INTERFACE|SPECIAL))|((GET|PUT)(FIELD|STATIC))|NEW|CHECKCAST|IF_?[A-Z]+)( 0x[[:xdigit:]]{2}){2}/$1 0x.. 0x../g;
s/LDC 0x[[:xdigit:]]{2}/LDC 0x../g;
s/^(OS|JVM|Classpath): .*/$1: .../;
s/\b[a-z]+\.class/xx.class/g;
s/L[a-z]+;/Lxx;/g;
s/ [a-z]+/ xx/g if /multiple matches: /;
s/-> instruction \d+/-> instruction .../;
