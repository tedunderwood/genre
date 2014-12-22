# R script to create precision-recall curves for poetry

curve <- read.csv('~/Dropbox/PythonScripts/training/alldata/confidence/smoothed.csv')
color = rep('black', length(curve$poerecall))
color[70] = 'red'
size = rep(1, length(j))
size[70] = 2
chroma = c('black', 'red')
df <- data.frame(recall = curve$poerecall, precision = curve$poeprecision, color = color, size = size)
q <- ggplot(df, aes(x=recall, y = precision, colour = color, size = size)) + geom_point() + scale_colour_manual(values = chroma) + 
  scale_size_continuous(range = c(2,5)) + theme(legend.position = 'none') + scale_y_continuous(limits = c(.895,1)) +
  theme(text = element_text(size = 20)) + ylab('precision\n') + xlab('\nrecall')
print(q)