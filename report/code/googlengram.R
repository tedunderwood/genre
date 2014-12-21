require(scales)
library(ggplot2)
df <- read.table('~/Dropbox/rutgers/8vofic.tsv', header = T, stringsAsFactors = F)
df$freq <- as.numeric(df$freq)

chromatic <- c('indianred3', 'lightsalmon4', 'olivedrab3')
p <-ggplot(df, aes(x=date, y=freq/100, group = ngram, colour = ngram)) + scale_colour_manual(values = chromatic)
p <- p + geom_line(size = 2) + ggtitle('Frequencies of tokens in\nGoogle\'s "English Fiction" collection\n')

p <- p + scale_x_continuous("") + scale_y_continuous("", limits = c(0, 0.00000175), labels = percent_format()) + 
  theme(legend.text = element_text(size=16), axis.text = element_text(size=16)) + theme(axis.title.y = element_text(vjust=0.2)) +
  theme(plot.title = element_text(size = 20, lineheight = 1.2)) + theme(text = element_text(size = 20))
print(p)