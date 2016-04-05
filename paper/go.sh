set -e
Rscript -e "require(knitr); knit('paper.Rnw')"
latexmk paper.tex -shell-escape -pdf -pv
