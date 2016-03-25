require(data.table)
require(plyr)
require(Hmisc)

# gets all speedups for a single host
# host-id, cores, speedup, spec
host_data_for_benchmark <- function(host, benchmark, dat, parfun_type) {
  host_data <- dat[dat$host.id == host & dat$spec.name %like% benchmark,]
  
  important <- c("cores", "host.id", "platform", "mean.runtime")
  
  serial_speeds   <- host_data[host_data$spec.name %like% "serial",important]
  parallel_speeds <- host_data[host_data$spec.name %like% parfun_type,important]
  
  # I already know these are for the same type of benchmark, since I filtered already
  d <- merge(serial_speeds, parallel_speeds, by=c("cores", "host.id", "platform"))
  
  # x is the serial one, y is the parallel speed
  d$speedup <- d$mean.runtime.x / d$mean.runtime.y
  
  d$benchmark <- rep(benchmark, nrow(d))
  
  return(d)
}

host_speedups_for_bench <- function(data, benchmark, parfun_type) {
  # for every host that we find, compute their average runtime on each test
  hosts <- unique(data$host.id)
  
  curr <- host_data_for_benchmark(hosts[1], benchmark, data, parfun_type)
  for (host in tail(hosts, -1)) {
    d <- host_data_for_benchmark(host, benchmark, data, parfun_type)
    curr <- rbind(curr, d)
  }
  
  return(curr)
}

plot_means_error_for_benchmark <- function(data, benchmark, parfun_type) {
  d <- host_speedups_for_bench(data, benchmark, parfun_type)[,c("cores", "speedup")]
  
  boxplot(speedup ~ cores, data=d,
          main=paste(parfun_type, "speedup for", benchmark, "from", nrow(d), "trials", sep=" "),
          xlab="number of cores",
          ylab="speedup w.r.t serial version"
  )
  
  means <- ddply(d, "cores", colwise(mean))
  sds <- ddply(d, "cores", colwise(sd))
  sumr <- merge(means, sds, "cores", suffixes=c(".mean", ".sd"))
  
  return(sumr)
}

plot_runtimes_scatter <- function(data, benchmark, parfun_type) {
  bs     <- data[data$spec.name %like% benchmark,]
  serial <- bs[bs$spec.name %like% "serial",c("cores", "mean.runtime")]
  par    <- bs[bs$spec.name %like% parfun_type,c("cores", "mean.runtime")]
  plot(par)
  points(serial, col="red")
}

data <- read.csv("data.csv", header=TRUE, sep=",")
# plot_means_error_for_benchmark(dat, "id3.small", "parfun")