Summary: ProjectSWG
Name: projectswg
Version: 0.9.1
Release: 1
License: Unknown
Vendor: projectswg.com
Prefix: /opt
Provides: projectswg
Requires: ld-linux.so.2 libX11.so.6 libXext.so.6 libXi.so.6 libXrender.so.1 libXtst.so.6 libasound.so.2 libc.so.6 libdl.so.2 libgcc_s.so.1 libm.so.6 libpthread.so.0 libthread_db.so.1
Autoprov: 0
Autoreq: 0

#avoid ARCH subfolder
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.%%{ARCH}.rpm

#comment line below to enable effective jar compression
#it could easily get your package size from 40 to 15Mb but 
#build time will substantially increase and it may require unpack200/system java to install
%define __jar_repack %{nil}

%description
ProjectSWG

%prep

%build

%install
rm -rf %{buildroot}
mkdir -p %{buildroot}/opt
cp -r %{_sourcedir}/ProjectSWG %{buildroot}/opt

%files
%doc /opt/ProjectSWG/app/LICENSE.txt
/opt/ProjectSWG

%post

xdg-desktop-menu install --novendor /opt/ProjectSWG/ProjectSWG.desktop

if [ "false" = "true" ]; then
    cp /opt/ProjectSWG/projectswg.init /etc/init.d/projectswg
    if [ -x "/etc/init.d/projectswg" ]; then
        /sbin/chkconfig --add projectswg
        if [ "false" = "true" ]; then
            /etc/init.d/projectswg start
        fi
    fi
fi

%preun

xdg-desktop-menu uninstall --novendor /opt/ProjectSWG/ProjectSWG.desktop

if [ "false" = "true" ]; then
    if [ -x "/etc/init.d/projectswg" ]; then
        if [ "true" = "true" ]; then
            /etc/init.d/projectswg stop
        fi
        /sbin/chkconfig --del projectswg
        rm -f /etc/init.d/projectswg
    fi
fi

%clean
