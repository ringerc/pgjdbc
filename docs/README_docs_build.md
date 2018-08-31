install redcarpet (rubygem)

    cd docs/documentation
    OUTDIR=pgjdbc-doc-42.2.4.1
    mkdir $OUTDIR
    cp head/*.html  $OUTDIR
    for f in $(find head -name \*.md); do redcarpet --parse tables --render tables $f > $OUTDIR/$(basename $f .md).html; done
    zip -r $OUTDIR.zip $OUTDIR
