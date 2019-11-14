Projekt je urobeny ako Maven project, takze dependencies sa vyriesia automaticky.
Najprv si synchronizujte tento git do nejakeho priecinku.

Idea fungovania aplikacie:
1. Clovek dostane pred seba formular, tam si moze zadat:
- subor, s ktorym chce pracovat (vyberie z vlastneho PC)
- verejny kluc pre asymetricku sifru (ktorou sa bude pred ulozenim sifrovat subor obsahujuci symetricky kluc).
2. Subor sa nahra na server:
- na serveri sa vygeneruje nahodny symetricky kluc, ktorym sa subor zasifruje pred ulozenim na server
- vygenerovany symetricky kluc sa ulozi do formatu fileName.key hned vedla nahraneho zasifrovaneho suboru fileName.nieco, pricom tento .key subor sam o sebe bude sifrovany asymetrickou sifrou s klucom poskytnutym od cloveka pri uploade
3. Po uspesnom nahrati na server sa cloveku spristupnia moznosti:
- iba stiahnut subor v jeho zasifrovanej forme
- stiahnut subor v jeho desifrovanej forme - podmienkou je, ze clovek zada svoj privatny kluc do asymetrickej sifry, ktorou server bude najprv vediet desifrovat subor, v ktorom sa nachadza KLUC symetrickej sifry suboru. Po tom, co server precita symetricky kluc pre subor pouzivatela, odsifruje ho ked ho posuva na download (subor cloveka vsak ostava zasifrovany na serveri)
- stiahnut aplikaciu, ktora bude vediet desifrovat subor



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


-------------------------------- Zadanie 5 ---------------------------------
Michal Moravek ----------
registracia - private public key, kluce ako subory - vytiahnut kluce zo suboru, zasifrovat, ulozit ako byte array
            - moznost pri registracii vygenerovat kluce
pri registracii vytvorit priecinok username
Adrian Blazicek -----------
nova tabulka files - nazov, path, majitel
tabulka comments - user, message, file, timestamp
v db private key uzivatela sifrovany jeho heslom (PBKDF2 s inym poctom iteracii) - symetricka sifra

MATO SUCHTER + JURO KOMINAK ------------
zdielanie suboro : vyberem subor
			vyberem usera
			zadam heslo - vytvori sa kluc (symetricky)
			subor sa nakopiruje druhemu uzivatelovi
			desifrujeme hlavicku nasim private key
			zasifrujeme hlavicku pub key druheho uzivatela

IGOR SZALAY: ----------
prehliadanie suborov:
		Igor - skusit rozbehat php - toto implementovat v tom ked tak
		dynamicky zoznam suborov + filtrovanie 
		template pre stranku suboru -> tabulka comments - user, message, file, timestamp
		

