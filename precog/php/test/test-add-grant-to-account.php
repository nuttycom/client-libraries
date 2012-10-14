<?php

require_once('basetest.php');

class AddGrantToAccountTest extends PrecogBaseTest {
    function testAddGrantToAccount() {
        $api = PrecogBaseTest::createApi($this->info);
        $result = $api->createKey(array("grants"=>array(array("type"=>"read", "path"=>$this->info["path"]."foo/", "ownerAccountId"=> $this->info["accountId"], "expirationDate"=> null))));
 		$apiKey1 = $result["apiKey"];

		$randomemail = "testphp.".rand(0, 100000000)."@precog.com";
        $account2 = $api->createAccount($randomemail, PrecogBaseTest::$password, $api->baseUrl, $api->version);
        $account2Id = $account2["data"]["accountId"];

        $result = $api->describeKey($apiKey1);
 		$grantId = $result["grants"][0]["grantId"];

 		$result = $api->addGrantToAccount(PrecogBaseTest::$email, PrecogBaseTest::$password, $account2Id, $grantId);
 		$this->assertTrue($result["ok"]);

 		$result = PrecogAPI::describeAccount($randomemail, PrecogBaseTest::$password, $account2Id, $this->info["baseUrl"], $this->info["version"]);

 		$api2 = new PrecogAPI($result["data"]["apiKey"], $this->info["baseUrl"], $this->info["version"]);

 		$result = $api2->describeKey($result["data"]["apiKey"]);

 		$found = false;
 		foreach ($result['grants'] as $key => $value) {
 			if($value['grantId'] === $grantId) {
 				$found = true;
 				break;
 			}
 		}
 		
 		$this->assertTrue($found);
    }
}
?>