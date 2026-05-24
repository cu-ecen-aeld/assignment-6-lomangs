inherit core-image
CORE_IMAGE_EXTRA_INSTALL += "aesd-assignments"
CORE_IMAGE_EXTRA_INSTALL += "openssh"
inherit extrausers
# See https://docs.yoctoproject.org/singleindex.html#extrausers-bbclass
# We set a default password of root to match our busybox instance setup
# Don't do this in a production image
# PASSWD below is set to the output of
# printf "%q" $(mkpasswd -m sha256crypt root) to hash the "root" password
# string
# 1. You must explicitly inherit the extrausers class
INHERIT += "extrausers"

#PASSWD = "\$5\$2WoxjAdaC2\$l4aj6Is.EWkD72Vt.byhM5qRtF9HcCM/5YpbxpmvNB5"
PASSWD = "\$6\$2.Nh8By3JVQflMHW\$S6ebbWO8Cx3VsDaszEAYwFLX1IbqtXtGakd0FOhGrboJKHaNtiUWpdh6/9hwuAnbfvlr72WZk6Uys7r75zVre1"
EXTRA_USERS_PARAMS = "usermod -p '${PASSWD}' root;"

allow_root_ssh() {
    if [ -e ${IMAGE_ROOTFS}/etc/ssh/sshd_config ]; then
            sed -i 's/#PermitRootLogin.*/PermitRootLogin yes/' ${IMAGE_ROOTFS}/etc/ssh/sshd_config
            sed -i 's/#PermitEmptyPasswords.*/PermitEmptyPasswords yes/' ${IMAGE_ROOTFS}/etc/ssh/sshd_config
     fi
}

# Forces OpenSSH to bypass password and root connection restrictions
ROOTFS_POSTPROCESS_COMMAND += "force_openssh_dev_access; "

force_openssh_dev_access() {
# 1. Re-create and secure the OpenSSH privilege separation sandbox directories
    mkdir -p ${IMAGE_ROOTFS}/var/run/sshd
    chown -R root:root ${IMAGE_ROOTFS}/var/run/sshd
    chmod 0755 ${IMAGE_ROOTFS}/var/run/sshd

    # 2. Setup the root user's secure .ssh directory inside the target image
    mkdir -p ${IMAGE_ROOTFS}/home/root/.ssh
    chmod 0700 ${IMAGE_ROOTFS}/home/root/.ssh
    chown -R root:root ${IMAGE_ROOTFS}/home/root/.ssh

    # 3. Inject your personal host computer's SSH public key
    # This reads the key from your host (~/.ssh/) during the BitBake build phase
    if [ -f ~/.ssh/id_ed25519.pub ]; then
        cat ~/.ssh/id_ed25519.pub > ${IMAGE_ROOTFS}/home/root/.ssh/authorized_keys
    elif [ -f ~/.ssh/id_rsa.pub ]; then
        cat ~/.ssh/id_rsa.pub > ${IMAGE_ROOTFS}/home/root/.ssh/authorized_keys
    fi

    # Secure the authorized_keys file
    if [ -f ${IMAGE_ROOTFS}/home/root/.ssh/authorized_keys ]; then
       chmod 0600 ${IMAGE_ROOTFS}/home/root/.ssh/authorized_keys
       chown root:root ${IMAGE_ROOTFS}/home/root/.ssh/authorized_keys
    fi

    # 4. Clean configuration injection (Removing the unsupported UsePAM)
    if [ -e ${IMAGE_ROOTFS}/etc/ssh/sshd_config ]; then
      sed -i '/PermitRootLogin/d' ${IMAGE_ROOTFS}/etc/ssh/sshd_config
      sed -i '/PermitEmptyPasswords/d' ${IMAGE_ROOTFS}/etc/ssh/sshd_config
      sed -i '/PasswordAuthentication/d' ${IMAGE_ROOTFS}/etc/ssh/sshd_config
      sed -i '/PubkeyAuthentication/d' ${IMAGE_ROOTFS}/etc/ssh/sshd_config
      sed -i '/UsePAM/d' ${IMAGE_ROOTFS}/etc/ssh/sshd_config

      echo "PermitRootLogin yes" >> ${IMAGE_ROOTFS}/etc/ssh/sshd_config
      echo "PasswordAuthentication yes" >> ${IMAGE_ROOTFS}/etc/ssh/sshd_config
      echo "PubkeyAuthentication yes" >> ${IMAGE_ROOTFS}/etc/ssh/sshd_config
   fi
}
