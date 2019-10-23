Projekt je urobeny ako Maven project, takze dependencies sa vyriesia automaticky.
Najprv si synchronizujte tento git do nejakeho priecinku.

Ako importovat do vasho IDE:
  1. ak mate NetBeans IDE:

    - File->Open Project, najdite zlozku kde ste inicializovali git, malo by to rozpoznat ako Maven project
    - Run->Build Project pre zbuildovanie (musi byt Build Success)
    - Run->Run, ked sa spyta kde to chcete deploynut tak Apache Tomcat 8 (co ide s predpokladom, ze mate EE ediciu NetBeans kde su aplikacne servre vstavane)
    - hotovo, bezi
  
  2. ak mate IntelliJ IDE:

    - File->New->Project from version control->Git, zadajte URL tohto repo aj s vasim loginom, stiahne vam to projekt a otvori ho
    - Build->Build project na zbuildovanie
    - kedze toto prostredie z neznamych dovodov nema vstavany aplikacny server, potrebujete stiahnut Apache Tomcat 8 separatne
    - ist na https://tomcat.apache.org/download-80.cgi, stiahnut Tomcat 8.5.47, 64-bit Windows zip, rozbalit hocikde do zlozky
    - pridat si potrebny Run Configuration do vasho IntelliJ takto (pri Select artifacts to deploy pridajte WAR nasho CryptoUPB): 
            https://www.mkyong.com/intellij/intellij-idea-run-debug-web-application-on-tomcat
    - spustit run config novu
    - hotovo, bezi
   
   3. ak mate Eclipse IDE:

    - ...
    - boh vam pomahaj
    

    - (seriozne ak to niekto ma mozem asi urobit aj na to guide, lol)
