install redcarpet (rubygem)

    OUTDIR=pgjdbc-doc-42.2.4_2ndq_r1_1
    cd documentation
    mkdir $OUTDIR
    cp head/*.html  $OUTDIR
    for f in $(find head -name \*.md); do redcarpet --parse tables --render tables $f $OUTDIR/$(basename $f .md).html; done
    zip -r $OUTDIR.zip $OUTDIR
