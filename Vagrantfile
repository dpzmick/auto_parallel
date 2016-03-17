# -*- mode: ruby -*-
# vi: set ft=ruby :

# creates n of each machine type
cores    = [1,2,4,8]
how_many = 4

boxes = []

starting_port = 9000
cores.each do |dcores|
    for n in 1..how_many
        box = {
            :name => "cores" + dcores.to_s + "n" + n.to_s,
            :port => starting_port + n + dcores * 100,
            :mem  => "1024",
            :cpu  => dcores.to_s
        }

        boxes << box
    end
end

Vagrant.configure(2) do |config|
  config.vm.box = "ubuntu/trusty64"
  config.vm.synced_folder '.', '/vagrant', disabled: true
  config.vm.provision "shell", path: "build_machine.sh"

  boxes.each do |opts|
    config.vm.define opts[:name] do |config|
      config.vm.hostname = opts[:name]

      config.vm.provider "virtualbox" do |v|
        v.customize ["modifyvm", :id, "--memory", opts[:mem]]
        v.customize ["modifyvm", :id, "--cpus", opts[:cpu]]
      end

      config.vm.network "forwarded_port", guest: 22, host: opts[:port], id: "ssh"
    end
  end
end
