library(ggplot2)
library(scales)
window <- function(money) {
  y = numeric()
  for (i in 1: length(money)) {
    startwindow = i-2
    endwindow = i+ 2
    if (startwindow < 1) startwindow = 1
    if (endwindow > length(money)) endwindow = length(money)
    thesum = 0
    for (j in startwindow: endwindow) {
      thesum = thesum + money[j]
    }
    y = c(y, thesum / (endwindow - startwindow))
  }
  y
}


rawdata <- read.csv('/Users/tunder/maps/pagesperyr4.csv', header = TRUE)
framelen = length(rawdata$date)
normalized <- data.frame(x = rawdata$date, y = c(window(rawdata$poetry/rawdata$totals), window(rawdata$fiction/rawdata$totals),
                         window(rawdata$drama/rawdata$totals), window(rawdata$ads/rawdata$totals)),
                         genre = c(rep('poetry', framelen), rep('fiction', framelen),
                                   rep('drama', framelen), rep('ads', framelen)))
chromatic <- c("gray20", "mediumpurple", "lightsteelblue", 'cadetblue4')
theme_set(theme_gray(base_size=14))
p <-ggplot(normalized, aes(x=x, y=y, group = genre, colour = genre)) + scale_colour_manual(values = chromatic)
p <- p + geom_area(aes(colour=genre, fill = genre), position = 'stack') + scale_fill_manual(values = chromatic)

p <- p + scale_x_continuous("") + scale_y_continuous("pages in genre as a percentage of the whole collection", labels=percent) + theme(axis.text = element_text(size=14)) + theme(axis.title.y = theme_text(vjust=0.2))
print(p)