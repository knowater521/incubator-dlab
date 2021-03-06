# *****************************************************************************
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# ******************************************************************************

locals {
  node_name = "${var.sbn}-nb-${var.notebook_name}"
  nic       = "${var.sbn}-nb-${var.notebook_name}-nic"
}

resource "azurerm_network_interface" "nic" {
    name                      = local.nic
    location                  = var.region
    resource_group_name       = var.resource_group
    network_security_group_id = var.nb-sg_id

    ip_configuration {
        name                          = "${local.nic}-IPconigurations"
        subnet_id                     = var.subnet_id
        private_ip_address_allocation = "dynamic"
    }

    tags = {
        Exploratory      = var.notebook_name
        SBN              = var.sbn
        Name             = local.node_name
        Project_name     = var.project_name
        Project_tag      = var.project_tag
        Endpoint_Tag     = var.endpoint_tag
        Product          = var.product
        User_Tag         = var.user_tag
        Custom_Tag       = var.custom_tag
    }
}

resource "azurerm_virtual_machine" "instance" {
    count                 = var.custom_ami  == true ? 0 : 1
    name                  = local.node_name
    location              = var.region
    resource_group_name   = var.resource_group
    network_interface_ids = ["${azurerm_network_interface.nic.id}"]
    vm_size               = var.instance_type

    storage_os_disk {
        name              = "${local.node_name}-volume-primary"
        caching           = "ReadWrite"
        create_option     = "FromImage"
        managed_disk_type = "Premium_LRS"
    }

    storage_image_reference {
        publisher = var.ami_publisher[var.os_env]
        offer     = var.ami_offer[var.os_env]
        sku       = var.ami_sku[var.os_env]
        version   = var.ami_version[var.os_env]
    }

    os_profile {
        computer_name  = local.node_name
        admin_username = var.initial_user
    }

    os_profile_linux_config {
        disable_password_authentication = true
        ssh_keys {
            path     = "/home/${var.initial_user}/.ssh/authorized_keys"
            key_data = "${file("${var.ssh_key}")}"
        }
    }

    tags = {
        Exploratory      = var.notebook_name
        SBN              = var.sbn
        Name             = local.node_name
        Project_name     = var.project_name
        Project_tag      = var.project_tag
        Endpoint_Tag     = var.endpoint_tag
        Product          = var.product
        User_Tag         = var.user_tag
        Custom_Tag       = var.custom_tag
    }
}

resource "azurerm_virtual_machine" "instance_custom" {
    count                 = var.custom_ami  == true ? 1 : 0
    name                  = local.node_name
    location              = var.region
    resource_group_name   = var.resource_group
    network_interface_ids = ["${azurerm_network_interface.nic.id}"]
    vm_size               = var.instance_type

    storage_os_disk {
        name              = "${local.node_name}-volume-primary"
        caching           = "ReadWrite"
        create_option     = "FromImage"
        managed_disk_type = "Premium_LRS"
    }

    storage_image_reference {
        id = var.ami
    }

    os_profile {
        computer_name  = local.node_name
        admin_username = var.initial_user
    }

    os_profile_linux_config {
        disable_password_authentication = true
        ssh_keys {
            path     = "/home/${var.initial_user}/.ssh/authorized_keys"
            key_data = "${file("${var.ssh_key}")}"
        }
    }

    tags = {
        Exploratory      = var.notebook_name
        SBN              = var.sbn
        Name             = local.node_name
        Project_name     = var.project_name
        Project_tag      = var.project_tag
        Endpoint_Tag     = var.endpoint_tag
        Product          = var.product
        User_Tag         = var.user_tag
        Custom_Tag       = var.custom_tag
    }
}