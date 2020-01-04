_This code was written as part of my bachelor's thesis. Please find the abstract below._

# mip-dtn-proxy
Bachelor Thesis #1: __A proxy for Military Data Communication over Delay- and Disruption-Tolerant Networks__

Joint operations between military forces are the most likely scenario nowadays. To accomplish missions successfully, the exchange of electronic data between heterogenous Command and Control Information System (C2IS) is needed. The Multilateral Interoperability Programme (MIP) defines a Joint Command Control and Consultation Information Exchange Data Model (JC3IEDM) and a Data Exchange Mechanism (DEM) to enable communication without seman- tic mismatches. The DEM is designed to work on top of UDP and TCP.

When operating in hostile networking environments with long delays and a high number of disruptions, the TCP/IP stack does not perform very well. Delay- and Disruption-Tolerant Networks (DTNs) use message switching instead of packet switching and do not require a predetermined end-to-end path. IBR-DTN is a well supported and well documented imple- mentation for the Bundle Protocol and the Bundle Security Protocol defined by DTN RFCs.

This work provides a transparent, application-aware proxy service for the DEM operating on top of IBR-DTN. All communication phases of the DEM are supported. Considerable effort was needed to make the discovery of DEM nodes work over the DTN. As a result no upfront con- nection information is needed. The use of DTN group endpoints allows populating mapping tables between the DEM and the DTN address space. These mappings are used subsequently for efficient peer-to-peer communication between DEM nodes over the DTN.

To be platform independent the proxy is implemented in Java employing an asynchronous messaging library.
During the verification process of the proxy all extended features of IBR-DTN are turned off to help focusing on the DEM proxy functionality. All verification steps are executed by a small Javascript test framework running in a virtual environment. The final test was done using production-ready DEMs to ensure compatibility with existing implementations.
Compared to similar approaches, this work does not use general purpose protocols like http or SMTP but focuses on the DEM protocol which is specific to the military domain.

Bachelor Thesis #2: __Performance of Delay- and Disruption Tolerant Networks over an emulated Combat-Net Radio (CNR) Channel__

Tactical radios are the workhorses for mobile ground troops to transmit voice and data. The con- tribution of this paper is to implement the transmission of bundle messages created by a Delay- and Disruption-Tolerant Network (DTN) daemon over an emulated Very High Frequency (VHF) radio channel and to measure the achievable transmission duration and data-rate for increas- ing distances. The channel is provided by the Extendable Mobile Ad-hoc Network Emula- tor (EMANE). All common properties of real radio devices are mapped onto the appropriate configuration parameters of the emulator. For distances between 0 and 26 km the transmission durations for files of relevant sizes from 256 to 8192 Bytes are measured. The computed data- rate decreases beyond a distance of about 10 km in a sublinear way up to a cut-off of about 27 km. Compared to values from literature, the emulation results prove reasonable.
