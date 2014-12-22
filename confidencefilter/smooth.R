# smooth precision-recall curves
library(zoo)
# we need the locf function

bounded <- function(avector) {
  avector[avector < 0] <- 0
  avector[avector > 1] <- 1
  avector
}

conf80 <- read.csv('/Users/tunder/output/confidence80.csv')
conf80 <- na.locf(conf80)
for (i in 1:8) {
  firstpass <- lowess(conf80[[i]], f = .3)$y
  secondpass <- bounded(firstpass)
  conf80[[i]] <- signif(secondpass, digits = 3)
}

write.csv(conf80, file = '/Users/tunder/Dropbox/PythonScripts/training/alldata/confidence/smoothed.csv', row.names = FALSE)