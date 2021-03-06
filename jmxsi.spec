Name:	jmxsi	
Version:	1.3.0
Release:	1%{?dist}
Summary:	JMX Shell Interface (jmxsi) command line interface JMX client
License:	GPLv3
URL:		https://github.com/g76r/jmxsi
Source0:	jmxsi.tar.gz
BuildArch:	noarch

%description
JMX Shell Interface (jmxsi) is a command line interface JMX client enabling to
access a local or remote JVM to read and change JMX attributes and to invoke
JMX operations.

It supports getting easily composite attributes (such as HeapMemoryUsage) and
bulk getting/setting/invoking on several objects at a time using * in object
name.

The package also contains a higher level tool for HornetQ message broker
administration.

%prep
%setup -q -c %{name}

%build

%install
mkdir -p %{buildroot}/usr/local/bin
cp jmxsi hornetqsi %{buildroot}/usr/local/bin/
mkdir -p %{buildroot}/usr/share/java
cp jmxsi.jar %{buildroot}/usr/share/java/
mkdir -p %{buildroot}/usr/share/bash-completion/completions
cp bash_completion/jmxsi  %{buildroot}/usr/share/bash-completion/completions/

%files
%defattr(755, root, root, 755)
/usr/local/bin/jmxsi
/usr/local/bin/hornetqsi
%defattr(644, root, root, 755)
/usr/share/java/jmxsi.jar
/usr/share/bash-completion/completions/jmxsi
%doc

%changelog
* Thu Jun 11 2015 Grégoire Barbier <devel@g76r.eu> - 1.3.0-1
- Releasing 1.3.0
* Tue May 26 2015 Grégoire Barbier <devel@g76r.eu> - 1.2.0-1
- Releasing 1.2.0
* Mon May 11 2015 Grégoire Barbier <devel@g76r.eu> - 1.1.0-1
- Releasing 1.1.0
* Mon May 4 2015 Grégoire Barbier <devel@g76r.eu> - 1.0.0-1
- Initial package
