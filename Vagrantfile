# -*- mode: ruby -*-
# vi: set ft=ruby :

# creates n of each machine type
cores    = [1,2,4]
how_many = 4

boxes = []

cores.each do |dcores|
    for n in 1..how_many
        box = {
            :name => "cores" + dcores.to_s + "n" + n.to_s,
            :mem  => "2048",
            :cpu  => dcores.to_s
        }

        boxes << box
    end
end

Vagrant.configure(2) do |config|
  config.vm.box = "ubuntu/trusty64"
  config.vm.synced_folder '.', '/vagrant', disabled: true
  config.vm.provision "shell", path: "build_machine.sh", privileged: false

  boxes.each do |opts|
    config.vm.define opts[:name] do |config|
      config.vm.hostname = opts[:name]

      config.vm.provider "virtualbox" do |v|
        v.customize ["modifyvm", :id, "--memory", opts[:mem]]
        v.customize ["modifyvm", :id, "--cpus", opts[:cpu]]
      end
    end
  end
end
