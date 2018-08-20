#!/bin/bash

if=$1
p=$2

yf=cache/${p}.yaml
mf=cache/${p}.md

csplit -q $if '/^[ 	]*$/' -f cache/$p -b '%03d.md'
mv cache/${p}000.md $yf

title="$(sed -n -e '/^title:/s/title: //p' $yf)"

cat >$mf <<EOF
# $title

EOF

cat cache/${p}001.md >>$mf
rm cache/${p}001.md

sed -i $mf -e 's/^#/\n#/g'

## Deal with HTML-embedded tables, if any

if grep -q '^<table ' $mf; then
    csplit -q $mf \
	   '/^<table /' '{*}'\
	   -f cache/$p -b '%03d.md'
    mv cache/${p}000.md $mf
    ## each fragment is a HTML table plus the rest
    for ff in cache/${p}???.md; do
	csplit -q $ff\
	       '/^</table>/1' \
	       -f ${ff} -b '%03d.md'
	rm $ff
	mv ${ff}000.md ${ff}000.html
	pandoc -o ${ff}000.md ${ff}000.html
	cat ${ff}000.md >> $mf
	cat ${ff}001.md >> $mf
	rm ${ff}000.html ${ff}000.md ${ff}001.md
    done
fi
