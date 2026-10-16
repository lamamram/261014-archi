## Template pour l'Atelier sur les Attributs de Qualité 

**Méthode** : Quality Attribute Workshop (QAW): Atelier avec les parties prenantes

### Étape 1 : Identifier les Attributs de Qualité Prioritaires

**Questions clés** :
- Quel est le temps de réponse pour consulter un article acceptable ? < 200 ms pour 95% des requêtes sur un scénario de charge normale
- quelle est la disponibilité exigée ? 99.9% avec une bascule < 1 minute
> REM: les métriques éditées ici sont guidées par le domaine d'activité (marché/concurrence) ET/ OU les autres experts "métiers" (admin / sécurité / conformité)
> REM: est ce que les questions sont couplées entre elles (ex. performance vs disponibilité) ou non. donc 1 ou 2 scénarios. ICI découplées


### Étape 2 : Définir les Scénarios de Qualité

**Format** :
> **Source** → **Stimulus** → **Artefact** → **Environnement** → **Réponse** → **Mesure**

- **Source** : utilisateur final
- **Stimulus** : requête HTTP (GET /produits/{id})
- **Artefact** : affichage complet de la page produit
- **Environnement** : scénario de charge normale
- **Réponse** : page produit affichée avant les temps attendus
- **Mesure** : < 200ms

### Étape 3 : Sélectionner les Tactiques


* Scénario 1:
  - cache:  Redis (file d'attente en mode clé/valeur) / memcached (hash)
  - mise en cache côté client: navigateur (localStorage / sessionStorage)
    + gérer l'entête HTTP Cache-Control de la réponse
  - CDN : cloudflare / akamai ... (Content Delivery Network)
  - pooling: Doctrine PHP côté backend/ORM ou pg_bouncer côté dba



### Étape 4 : Évaluer les Trade-offs (compromis) et les Risques

**Méthode ATAM** (Architecture Tradeoff Analysis Method) :

* Identifier les trade-offs et Documenter les risques, et les attéanuations possibles.
1. cache:
   - compromis: performance UP / cohérence DOWN
   - risque: données obsolètes avant check réél avant de payer. trop tard et friction utilisateur
   - atténuation:
     + vérifier la vérité au plus tôt : début du tunnel de transformation
     + déterminer la valeur du TTL (Time To Live) 30 secondes


### Étape 5 : Validation des Scénarios de Qualité

* tests de performance, de sécurité, de fiabilité, etc.
```bash
## mesurer le temps de répone moyenne pour 95% de 100 requêtes
jmeter -n -t test_plan.jmx -l 
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6.3">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="Plan de Test Charge 100" enabled="true">
      <stringProp name="TestPlan.comments"></stringProp>
      <boolProp name="TestPlan.functional_mode">false</boolProp>
      <boolProp name="TestPlan.tearDown_on_shutdown">true</boolProp>
      <serializeThreadGroups>false</serializeThreadGroups>
      <elementProp name="TestPlan.user_defined_variables" elementType="Arguments" guiclass="ArgumentsPanel" testclass="Arguments" testname="Variables définies par l'utilisateur" enabled="true">
        <collectionProp name="Arguments.arguments"/>
      </elementProp>
    </TestPlan>
    <hashTree>
      <!-- Groupe d'unités configuré pour 100 utilisateurs virtuels -->
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="Groupe de 100 Utilisateurs" enabled="true">
        <intProp name="ThreadGroup.num_threads">100</intProp> <!-- 100 Connexions -->
        <intProp name="ThreadGroup.ramp_time">10</intProp>   <!-- Montée en charge en 10 secondes (10 users/sec) -->
        <longProp name="ThreadGroup.duration">60</longProp>   <!-- Durée du test : 60 secondes -->
        <boolProp name="ThreadGroup.same_user_on_next_iteration">true</boolProp>
        <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
        <elementProp name="ThreadGroup.main_controller" elementType="LoopController" guiclass="LoopControlPanel" testclass="LoopController" testname="Contrôleur Boucle" enabled="true">
          <intProp name="LoopController.loops">-1</intProp> <!-- Boucle infinie pendant la durée du test -->
          <boolProp name="LoopController.continue_forever">false</boolProp>
        </elementProp>
      </ThreadGroup>
      <hashTree>
        <!-- Paramètres par défaut pour les requêtes HTTP -->
        <ConfigTestElement guiclass="HttpDefaultsGui" testclass="ConfigTestElement" testname="Paramètres HTTP par défaut" enabled="true">
          <stringProp name="HTTPSampler.domain">://typicode.com</stringProp> <!-- Remplacez par votre domaine -->
          <stringProp name="HTTPSampler.protocol">https</stringProp>
          <stringProp name="HTTPSampler.path"></stringProp>
        </ConfigTestElement>
        <hashTree/>
        <!-- Requête HTTP à tester -->
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="Requête GET /posts" enabled="true">
          <stringProp name="HTTPSampler.path">/posts</stringProp> <!-- Remplacez par votre endpoint -->
          <stringProp name="HTTPSampler.method">GET</stringProp>
          <boolProp name="HTTPSampler.follow_redirects">true</boolProp>
          <boolProp name="HTTPSampler.auto_redirects">false</boolProp>
          <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
          <boolProp name="HTTPSampler.DO_MULTIPART_POST">false</boolProp>
          <boolProp name="HTTPSampler.BROWSER_COMPATIBLE_MULTIPART">false</boolProp>
          <boolProp name="HTTPSampler.image_parser">false</boolProp>
          <boolProp name="HTTPSampler.concurrentDwn">false</boolProp>
          <stringProp name="HTTPSampler.concurrentPool">6</stringProp>
          <boolProp name="HTTPSampler.md5">false</boolProp>
          <intProp name="HTTPSampler.ipSourceType">0</intProp>
        </HTTPSamplerProxy>
        <hashTree/>
        <!-- Récepteur : Rapport de synthèse (Idéal pour voir les temps de réponse en CLI/GUI) -->
        <ResultCollector guiclass="StatVisualizer" testclass="ResultCollector" testname="Rapport de synthèse" enabled="true">
          <boolProp name="ResultCollector.error_logging">false</boolProp>
          <objProp>
            <name>saveConfig</name>
            <value class="SampleSaveConfiguration">
              <time>true</time>
              <latency>true</latency>
              <timestamp>true</timestamp>
              <success>true</success>
              <label>true</label>
              <code>true</code>
              <message>true</message>
              <threadName>true</threadName>
              <dataType>true</dataType>
              <encoding>false</encoding>
              <assertions>true</assertions>
              <subresults>true</subresults>
              <responseData>false</responseData>
              <samplerData>false</samplerData>
              <xml>false</xml>
              <fieldNames>true</fieldNames>
              <responseHeaders>false</responseHeaders>
              <requestHeaders>false</requestHeaders>
              <responseDataOnError>false</responseDataOnError>
              <saveAssertionResultsFailureMessage>true</saveAssertionResultsFailureMessage>
              <assertionsResultsToSave>0</assertionsResultsToSave>
              <bytes>true</bytes>
              <sentBytes>true</sentBytes>
              <url>true</url>
              <threadCounts>true</threadCounts>
              <idleTime>true</idleTime>
              <connectTime>true</connectTime>
            </value>
          </objProp>
          <stringProp name="filename"></stringProp>
        </ResultCollector>
        <hashTree/>
      </hashTree>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```


### Étape 6 : Documenter les Décisions (ADR)




