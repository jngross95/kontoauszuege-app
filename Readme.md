
sudo ln -sf /mnt/wslg/.X11-unix/X0 /tmp/.X11-unix/X0 && export DISPLAY=:0

# development build
mvn spring-boot:run




# production build
mvn clean package -Dvaadin.productionMode=true -Pproduction
target/start.sh

String dstName,
            String dstBic,
            String dstIban,
            BigDecimal btgValue,
            String endToEndId,
            String usage


cp kontoauszuege-app.desktop ~/.local/share/applications/kontoauszuege-app.desktop


# Githubpages
https://jngross95.github.io/kontoauszuege-app/


Versionen:
aktuell 25.08
https://github.com/flathub/org.electronjs.Electron2.BaseApp

achtung version taucht auch in der build.yml auf:
sudo flatpak install --system -y flathub org.freedesktop.Platform//25.08 org.freedesktop.Sdk//25.08 org.electronjs.Electron2.BaseApp//25.08 || true



find . -type f -exec grep -n "3.1.2" {} +
flatpak info --show-location com.example.kontoauszuege


python3 -m http.server 8080 --directory repo/
flatpak remote-add --user --no-gpg-verify local-app http://localhost:8080