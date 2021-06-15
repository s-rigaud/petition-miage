<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html xmlns="http://www.w3.org/1999/xhtml" lang="en">
	<head>
		<meta charset="utf-8" />
		<meta name="google-signin-scope" content="profile email">
		<meta name="google-signin-client_id"
			content="783652474514-hsrkuk75ikl453pu5fq2nf0m43q3qcsi.apps.googleusercontent.com" />
		<script src="https://apis.google.com/js/platform.js"></script>
		
		<meta name="viewport" content="width=device-width, initial-scale=1">
		
		
		<title>Miage Petition Generator 2k21</title>
	</head>

	<body>
		<div class="g-signin2" data-onsuccess="onSignIn" data-theme="dark"></div>
		<script>
	        function onSignIn(googleUser) {

		        let profile = googleUser.getBasicProfile();
		        console.log("ID: " + profile.getId()); // Don't send this directly to your server!
		        console.log('Full Name: ' + profile.getName());
		        console.log('Given Name: ' + profile.getGivenName());
		        console.log('Family Name: ' + profile.getFamilyName());
		        console.log("Image URL: " + profile.getImageUrl());
		        console.log("Email: " + profile.getEmail());

		        let id_token = googleUser.getAuthResponse().id_token;
		        console.log("ID Token: " + id_token);
	        }
	    </script>


		<h1>
			Welcome to the super <strong>Miage Petition Generator 2k21</strong>
		</h1>

		<p>Available functionalities:</p>
		<br />

		<p>While not connected :</p>
		<ol>
			<li><a href='/_ah/api/petitions/create/fake'>Populate with
					fake petitions</a></li>

			<li><a href='/_ah/api/petitions/top10'>List 10 best
					petitions</a></li>
			<li>List 10 best petitions by tags :
				https://petitions-31032021.ew.r.appspot.com/petitions?tag=1</li>
			<li><form action="/_ah/api/petition" method="GET">
					<label for="fname">Petition by key:</label> <br /> <input
						type="text" id="id" name="id" value="PetitionKey" /> <input
						type="submit" value="Submit" />
				</form></li>
			<li><form action="/_ah/api/petitions" method="GET">
					<label for="fname">Petition by key:</label> <br /> <input
						type="text" id="tags" name="tags" value="[tag1, tag2]" /> <input
						type="submit" value="Submit" />
				</form></li>
		</ol>
		<br />
	
		<p>While connected :</p>
		<ol>
			<li>
				<p>Create a new petition</p>
				<form action="/_ah/api/petitions" method="POST">
					<label for="fname">Petition name:</label> <br /> <input type="text"
						id="name" name="name" value="Basic name" /> <input type="submit"
						value="Submit" />
				</form>
			</li>
			<li><a href='/_ah/api/me/petitions'>Get petitions I created</a></li>
		</ol>
	
	</body>
</html>