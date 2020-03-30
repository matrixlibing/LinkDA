#encoding: utf-8
import random
import yaml

number = ['0','1','2','3','4','5','6','7','8','9']
alpnum = number + ['a', 'b', 'c', 'd', 'e', 'f']

def getImei():
	imei = ""
	for i in range(0, 15):
		imei += random.choice(number)
	return imei

def getMac():
	mac = ""
	for i in range(0,6):
		mac += ''.join(random.sample(alpnum, 2))
		mac += ":"
	return mac[0:len(mac) - 1]

#讯飞数据
xf = open("voiceads/file.txt", "w")
template = "%s {\"gid\":[%s]}"
for i in range(0,1000):
	mac = getMac()
	gidset = set()
	for j in range(0, random.randint(1, 3)):
		gidset.add(random.choice(['1','2','3']))
	record = template % (mac, ','.join(gidset))
	xf.write(record + "\n")
xf.close()

#欢网数据
hw = open("huanwang/file.txt", "w")
template = "%s {\"ottmac\":[%s],\"gid\":[%s]}"
for i in range(0,1000):
	mac = getMac()
	if random.random() > 0.5:
		with open("voiceads/file.txt") as fo:
			line = random.choice(fo.readlines())
			mac = line.split()[0]
	ottmac = set()
	for j in range(0, random.randint(1, 3)):
		ottmac.add('"' + getMac() + '"')
	gidset = set()
	for j in range(0, random.randint(1, 3)):
		gidset.add(random.choice(['1','2','3']))
	record = template % (mac, ','.join(ottmac), ','.join(gidset))
	hw.write(record + "\n")
hw.close()
